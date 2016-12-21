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
#include "opencv2/calib3d/calib3d.hpp"
#include "opencv2/ml/ml.hpp"
#include <opencv2/flann/flann.hpp>
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
    //	Loading Inverted Index File
    //=======================================
    ifstream InvertedIndexFile("invertedindexfile_5500.txt",ios::in);

    map< int, map< int, int > > InvertedFile;

    LOGI("ProgressCheck: InvertedFile open status: %d", InvertedIndexFile.is_open());

    int vword, vcount, vimage, vnum;
    while(InvertedIndexFile.good()){
        InvertedIndexFile >> vword >> vcount;		//loading inverted index
        for(int i = 0 ; i < vcount ; i++ ){
            InvertedIndexFile >> vimage >> vnum ;
            InvertedFile[vword][vimage] = vnum;
        }
/*		if( InvertedFile[ vword ].size() > 500 ){
			InvertedFile.erase( vword );
			prune_count++;
		}
*/	}
    InvertedIndexFile.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","InvertedIndexFile Loaded");
    LOGI("ProgressCheck: InvertedFile size= %d",(int)InvertedFile.size());

    //=======================================
    //	Loading the HKMeans Tree
    //=======================================
    ifstream t_file("HKMeans_10_4n.Tree",ios::in);	//loading the tree
    LOGI("ProgressCheck: Tree file open status: %d", t_file.is_open());
    Tree* tree = ParseTree(t_file,0);				//parsing the tree
    t_file.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","HKMeans tree Loaded");

    //======================================================
    //	Loading Image file names and Term Frequency Count
    //======================================================
    string imageListFileName = "../GolkondaImages_5489.txt";
    vector < string > ImageList;
    string temp;
    ifstream imageListFile;
    imageListFile.open(imageListFileName.c_str(),ios::in);
    string TF_filename = "descCount.txt";
    vector < int > TF;
    int tempTF;
    ifstream TF_file;
    TF_file.open(TF_filename.c_str(), ios::in );
    for(int i = 1 ; i <= 5500 ; i++ ){
        imageListFile >> temp;
        ImageList.push_back( temp );		//all the image names go into ImageList
        TF_file >> tempTF;
        TF.push_back(tempTF);				//all the descriptor counts go into TF
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




extern "C" {
jstring Java_com_example_home_BequestProto_MainActivity_GetMatch(JNIEnv *env, jobject thiz,jint width, jint height, jbyteArray yuv, jintArray bgra) {


    /* PRE pROCESSING */
    QWords.clear();
    QueryHist.clear();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Start...");
/* PRE pROCESSING ends*/
    //=======================================
    //	Loading the frame
    //=======================================
    //		Mat* pMatGr=(Mat*)addrGray;
    //	    Mat* pMatRgb=(Mat*)addrRgba;

    //		Mat img_temp = *pMatGr;
    //		Mat img_temp = imread("/data/data/com.example.heritagecam/files/TestImage.jpg",CV_LOAD_IMAGE_GRAYSCALE);



    /* Take and Process Photo STARTS */


    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);

    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
    Mat img_temp(height, width, CV_8UC1, (unsigned char *)_yuv);

    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);


/*	Mat img_temp = imread("/sdcard/TestImage.jpg",CV_LOAD_IMAGE_GRAYSCALE);

	int height = img_temp.rows;
	int width = img_temp.cols;
*/

    Mat img;
    if( height > 400 || width > 400 ){
        int new_h = 250;
        int new_w = (new_h*width)/height;
        img.create(new_h,new_w,CV_8UC3);
        resize(img_temp,img,img.size(),0,0, INTER_LINEAR);
    }else{
        img = img_temp.clone();
    }

    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Frame Loaded");



    //SiftFeatureDetector detector;
    std::vector<KeyPoint> keypoints;

   // SiftDescriptorExtractor extractor;
    Mat descriptors;

    //detector.detect(img,keypoints);
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","SIFT detection");

    //extractor.compute(img,keypoints,descriptors);
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","SIFT descriptor extraction");


//    SiftFeatureDetector detector;
//    std::vector<KeyPoint> keypoints;

//    SiftDescriptorExtractor extractor;
//    Mat descriptors;



}
}






extern "C"
jstring Java_com_example_home_BequestProto_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
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

