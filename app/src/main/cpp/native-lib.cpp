#include<jni.h>
#include <stdio.h>
#include <algorithm>
#include <iostream>
#include <ctime>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <map>
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
//#include "opencv2/nonfree/features2d.hpp"
#include "opencv2/ml/ml.hpp"
#include "opencv2/calib3d/calib3d.hpp"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/time.h>
#include <fstream>
#include <android/log.h>

using namespace std;
using namespace cv;

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native-activity", __VA_ARGS__))

typedef struct Tree{
    int centers[10][128];
    struct Tree* sub[10];
    int nodes[10];
    int n_centers;
    int n_sub;
    int depth;
}Tree;


typedef struct RImage{
    int index;
    double r_score;
    double m_score;
    vector< Point2f > corners;
}RImage;

typedef struct obj{
    string name;
    double score;
}Object;

Tree* ParseTree(ifstream &t_file, int d){
    if( !t_file.good() ){
        return NULL;
    }
    int centers, subs;
    t_file >> centers >> subs;
    Tree *t = (Tree*)malloc(sizeof(Tree));
    t->n_centers = centers;
    t->n_sub = subs;
    t->depth = d;
    for(int i = 0 ; i < centers ; i++ ){
        for(int j = 0 ; j < 128 ; j++ ){
            t_file >> t->centers[i][j];
        }
    }
    for(int i = 0 ; i < subs ; i++ ){
        t->sub[i] = ParseTree(t_file,d+1);
    }
    for(int i = subs ; i < 10 ; i++){
        t->sub[i] = NULL;
    }
    return t;
}

void GetPath(Tree* tree , int* center , int* path , int depth){
    if(tree == NULL){
        path[depth] = -1;
        return;
    }
    double minD = 100000;
    double tempD;
    int minC = 2;
    for(int i = 0 ; i < tree->n_centers ; i++ ){
        tempD = 0;
        for(int j = 0 ; j < 128 ; j++ ){
            tempD += pow((double)(tree->centers[i][j] - center[j]),2.0);
        }
        tempD = sqrt(tempD);
        if(tempD < minD){
            minD = tempD;
            minC = i;
        }
    }
    path[depth] = minC+1;
    GetPath(tree->sub[minC], center, path, depth+1);
}

int GetWord( int *path ){
    int w = 0;
    for( int i = 0 ; i < 3 ; i++ ){
        //		cout << path[i] << " ";
        w = (w*10) + path[i] - 1;
    }
    w += 1;
    //	cout << w << endl;
    return w;
}


int compare ( const void *p1 , const void *p2 ){
    double p1_c = (* (RImage *)p1).r_score;
    double p2_c = (* (RImage *)p2).r_score;
    return p1_c - p2_c < 0 ? 1 : -1;
}

int compare2 ( const void *p1 , const void *p2 ){
    double p1_c = (* (Object *)p1).score;
    double p2_c = (* (Object *)p2).score;
    return p1_c - p2_c < 0 ? 1 : -1;
}


int isInside ( int x , int y , int x1 , int y1  , int x3 , int y3 )
{
    if ( x >= x1 && x <= x3 ){
        if ( y >= y1 && y <= y3 ){
            return 1 ;
        }
    }
    return 0;
}

int findInt ( string c)
{
    int a = 0 ;
    int i = 0 ;
    while ( c[5+i] != '.' )
    {
        a = a*10 + (int)(c[5 + i] - '0') ;
        i++;
    }
    return a ;
}


int N,N_orig;
map< int, map< int, int > > InvertedFile;
Tree *tree;
vector < string > ImageList;
vector < string > Annotations;
vector < int > TF;
map< int, int > QueryHist;
vector < int > QWords;
int O;
map< string, int > Objects;
map<string, vector<string> > ObjectParts;


extern "C" {
JNIEXPORT void JNICALL Java_com_example_home_BequestProto_MainActivity_LoadData(JNIEnv* env, jobject thiz){

    //=======================================
    //	Loading #images in database
    //=======================================
    ifstream NumImagesFile("/sdcard/Carz/NumImages.txt");
    LOGI("ProgressCheck: NumImages open status: %d", NumImagesFile.is_open());
    NumImagesFile >> N;
    N_orig = N;
    NumImagesFile.close();

    //=======================================
    //	Loading distinct objects in database
    //=======================================
    ifstream ObjectsFile("/sdcard/Carz/Objects.txt");
    string tempobj, temppart; int tempcount;
    LOGI("ProgressCheck: Objects open status: %d", ObjectsFile.is_open());
    ObjectsFile >> O;
    for( int i = 0 ; i < O ; i++ ){
        ObjectsFile >> tempcount >> tempobj;
        Objects[tempobj] = tempcount;
    }
    ObjectsFile.close();

    //=======================================
    //	Loading distinct objects in database
    //=======================================


    //=======================================
    //	Loading Inverted Index File
    //=======================================
    ifstream InvertedIndexFile("/sdcard/Carz/InvertedIndex.txt",ios::in);

    LOGI("ProgressCheck: InvertedFile open status: %d", InvertedIndexFile.is_open());
    int vword, vcount, vimage, vnum;
    while(InvertedIndexFile.good()){
        InvertedIndexFile >> vword >> vcount;
        for(int i = 0 ; i < vcount ; i++ ){
            InvertedIndexFile >> vimage >> vnum ;
            InvertedFile[vword][vimage] = vnum;
        }
    }
    InvertedIndexFile.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","InvertedIndexFile Loaded");
    LOGI("ProgressCheck: InvertedFile size= %d",(int)InvertedFile.size());

    //=======================================
    //	Loading the HKMeans Tree
    //=======================================
    ifstream t_file("/sdcard/Carz/HKMeans_1000.Tree",ios::in);
    LOGI("ProgressCheck: Tree file open status: %d", t_file.is_open());
    tree = ParseTree(t_file,0);
    t_file.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","HKMeans tree Loaded");

    //======================================================
    //	Loading Image file names and Term Frequency Count
    //======================================================
    string imageListFileName = "/sdcard/Carz/Annotations.txt";
    string temp, tempLine;
    ifstream imageListFile;
    imageListFile.open(imageListFileName.c_str(),ios::in);
    string TF_filename = "/sdcard/Carz/DCount.txt";
    int tempTF;
    ifstream TF_file;
    TF_file.open(TF_filename.c_str(), ios::in );

    for(int i = 1 ; i <= N ; i++ ){
        imageListFile >> temp;
        getline( imageListFile, tempLine);
        ImageList.push_back( temp );
        Annotations.push_back( tempLine );
        TF_file >> tempTF;
        TF.push_back(tempTF);

    }
    //=======================================
    //	Loading Image file names
    //=======================================
    /*         ifstream an1_file("/sdcard/Golkonda_5500/AnnInfo.txt",ios::in);
         ifstream an2_file("/sdcard/Golkonda_5500/AnnText.txt",ios::in);
         ifstream an3_file("/sdcard/Golkonda_5500/AnnBoundary.txt",ios::in);
         int TotalAnn;
         an1_file>>TotalAnn;
         string TempImageId;
         for( int i = 0 ; i < TotalAnn ; i++ ){
                Annotation TempAnn;
                an1_file >> TempImageId >> TempAnn.type ;
                getline( an2_file , TempAnn.text );
                if( TempAnn.type.compare("OBJECT") == 0 ){
                       an3_file >> TempAnn.boundary;
                }
                ImageAnns[ TempImageId ].push_back(TempAnn);
         }
         an1_file.close();
         an2_file.close();
         an3_file.close();

     */




}
}



extern "C"
jstring
Java_com_example_home_BequestProto_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Welcome to C++";
    return env->NewStringUTF(hello.c_str());
}



extern "C"
{
void JNICALL Java_com_example_home_BequestProto_CameraActivity_salt(JNIEnv *env, jobject instance,
                                                                           jlong matAddrGray,
                                                                           jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }


}
}

