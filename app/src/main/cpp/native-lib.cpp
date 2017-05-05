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
#include "opencv2/nonfree/nonfree.hpp"
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

int level_nodes[] = {1, 11, 111, 1111, 11111, 111111};
int ten_powers[] = {0, 10, 100, 1000, 10000};


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
/**
typedef struct obj{
    string name;
    double score;
}Object;
**/
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

void GetPath(Tree* tree , int* center , int* path , int depth, int *hist){
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
	path[depth] = minC;
	int temp = 2*level_nodes[depth] + minC;
	for(int i = 0 ; i < depth ; i++){
		temp += (path[i]-1) * ten_powers[depth-i];
	}
	hist[depth+1] = temp;
    GetPath(tree->sub[minC], center, path, depth+1,hist);
}


int compare ( const void *p1 , const void *p2 ){
    double p1_c = (* (RImage *)p1).r_score;
    double p2_c = (* (RImage *)p2).r_score;
    return p1_c - p2_c < 0 ? 1 : -1;
}

int compare2 ( const void *p1 , const void *p2 ){
    double p1_c = (* (RImage *)p1).m_score;
    double p2_c = (* (RImage *)p2).m_score;
    return p1_c < p2_c ? 1 : -1;
}

extern "C"
void GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes) {
    if (!AStr) {
        ARes.clear();
        return;
    }

    const char *s = AEnv->GetStringUTFChars(AStr,NULL);
    ARes=s;
    AEnv->ReleaseStringUTFChars(AStr,s);
}


int N,N_orig;
map< int, map< int, int > > InvertedFile;
Tree *tree;
vector < string > ImageList;
vector < string > Annotations;
vector < int > TF;
map< int, int > QueryHist;
vector < int > QWords;
std::vector<KeyPoint> keypoints;
RImage *ImagesRetrieved;

extern "C" {
JNIEXPORT void JNICALL
Java_in_ac_iiit_cvit_bequest_MainActivity_LoadData(JNIEnv *env, jobject thiz, jstring fileLocation)
{

    //=======================================
    //	Loading #images in database
    //=======================================
    string str;
    GetJStringContent(env, fileLocation, str);
/*
    String numImage = str + "/BequestProto/NumImages.txt";

    ifstream NumImagesFile(numImage.c_str());
    LOGI("ProgressCheck: NumImages open status: %d", NumImagesFile.is_open());
    NumImagesFile >> N;
    N_orig = N;
    NumImagesFile.close();
*/
    N = 5500;
    N_orig = N;

    //=======================================
    //	Loading Inverted Index File
    //=======================================
    String Iindex = str + "/BequestProto/invertedindexfile_5500.txt";
    ifstream InvertedIndexFile(Iindex.c_str(),ios::in);

    LOGI("ProgressCheck: InvertedFile open status: %d", InvertedIndexFile.is_open());

    int prune_count = 0;
    int vword, vcount, vimage, vnum;
    while(InvertedIndexFile.good()){
        InvertedIndexFile >> vword >> vcount;		//loading inverted index
        for(int i = 0 ; i < vcount ; i++ ){
            InvertedIndexFile >> vimage >> vnum ;
            InvertedFile[vword][vimage] = vnum;
        }
		if( InvertedFile[ vword ].size() > 500 ){
			InvertedFile.erase( vword );
			prune_count++;
		}
	}
    InvertedIndexFile.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","InvertedIndexFile Loaded");
    LOGI("ProgressCheck: InvertedFile size= %d",(int)InvertedFile.size());
    LOGI("ProgressCheck: Prune count is %d",prune_count);

    //=======================================
    //	Loading the HKMeans Tree
    //=======================================
    String Hktree = str +"/BequestProto/HKMeans_10_4n.Tree";
    ifstream t_file(Hktree.c_str(),ios::in);	//loading the tree
    LOGI("ProgressCheck: Tree file open status: %d", t_file.is_open());
    tree = ParseTree(t_file,0);				//parsing the tree
    t_file.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","HKMeans tree Loaded");

    //======================================================
    //	Loading Image file names and Term Frequency Count
    //======================================================
    string imageListFileName = str + "/BequestProto/GolkondaImages_5500.txt";

    string temp;
    ifstream imageListFile;
    imageListFile.open(imageListFileName.c_str(),ios::in);
    LOGI("ProgressCheck: Name list file open status: %d", imageListFile.is_open());
    string TF_filename = str + "/BequestProto/descCount.txt";

    int tempTF;
    ifstream TF_file;
    TF_file.open(TF_filename.c_str(), ios::in );
    LOGI("ProgressCheck: Desc Count file open status: %d", TF_file.is_open());

    for(int i = 1 ; i <= N ; i++ ){
        imageListFile >> temp;
        ImageList.push_back( temp );		//all the image names go into ImageList
//        LOGI("ProgressCheck: Checking ImageList[i]: %s", ImageList[i-1].c_str() );
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
JNIEXPORT void JNICALL
Java_in_ac_iiit_cvit_bequest_MainActivity_LoadMyData(JNIEnv *env, jobject thiz,
                                                     jstring fileLocation)
{

    //=======================================
    //	Loading #images in database
    //=======================================
    string str;
    GetJStringContent(env, fileLocation, str);

    N = 978;
    N_orig = N;

    //=======================================
    //	Loading Inverted Index File
    //=======================================
    String Iindex = str + "/BequestProto2/InvertedIndex_10000_10.txt";
    ifstream InvertedIndexFile(Iindex.c_str(),ios::in);

    LOGI("ProgressCheck: InvertedFile open status: %d", InvertedIndexFile.is_open());

    int prune_count = 0;
    int vword, vcount, vimage, vnum;
    while(InvertedIndexFile.good()){
        InvertedIndexFile >> vword >> vcount;		//loading inverted index
        for(int i = 0 ; i < vcount ; i++ ){
            InvertedIndexFile >> vimage >> vnum ;
            InvertedFile[vword][vimage] = vnum;
        }
//        LOGI("ProgressCheck: InvertedFile[%d].size()= %d",vword,InvertedFile[vword].size());
        if( InvertedFile[ vword ].size() > 500 ){
            InvertedFile.erase( vword );
            prune_count++;
        }
    }
    InvertedIndexFile.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","InvertedIndexFile Loaded");
    LOGI("ProgressCheck: InvertedFile size= %d",(int)InvertedFile.size());
    LOGI("ProgressCheck: Prune count is %d",prune_count);

    //=======================================
    //	Loading the HKMeans Tree
    //=======================================
    String Hktree = str +"/BequestProto2/HKMeans_10000_10.Tree";
    ifstream t_file(Hktree.c_str(),ios::in);	//loading the tree
    LOGI("ProgressCheck: Tree file open status: %d", t_file.is_open());
    tree = ParseTree(t_file,0);				//parsing the tree
    t_file.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","HKMeans tree Loaded");

    //======================================================
    //	Loading Image file names and Term Frequency Count
    //======================================================
    string imageListFileName = str + "/BequestProto2/image_names.txt";

    string temp;
    ifstream imageListFile;
    imageListFile.open(imageListFileName.c_str(),ios::in);
    LOGI("ProgressCheck: Name list file open status: %d", imageListFile.is_open());
    string TF_filename = str + "/BequestProto2/descCount.txt";

    int tempTF;
    ifstream TF_file;
    TF_file.open(TF_filename.c_str(), ios::in );
    LOGI("ProgressCheck: Desc Count file open status: %d", TF_file.is_open());

    for(int i = 1 ; i <= N ; i++ ){
        imageListFile >> temp;
        ImageList.push_back( temp );		//all the image names go into ImageList
//        LOGI("ProgressCheck: Checking ImageList[i]: %s", ImageList[i-1].c_str() );
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
JNIEXPORT void JNICALL
Java_in_ac_iiit_cvit_bequest_PackageDownloader_LoadMyData(JNIEnv *env, jobject thiz,
                                                          jstring fileLocation)
{

    //=======================================
    //	Loading #images in database
    //=======================================
    string str;
    GetJStringContent(env, fileLocation, str);

    N = 978;
    N_orig = N;

    //=======================================
    //	Loading Inverted Index File
    //=======================================
    String Iindex = str + "/BequestProto2/InvertedIndex_10000_10.txt";
    ifstream InvertedIndexFile(Iindex.c_str(),ios::in);

    LOGI("ProgressCheck: InvertedFile open status: %d", InvertedIndexFile.is_open());

    int prune_count = 0;
    int vword, vcount, vimage, vnum;
    while(InvertedIndexFile.good()){
        InvertedIndexFile >> vword >> vcount;		//loading inverted index
        for(int i = 0 ; i < vcount ; i++ ){
            InvertedIndexFile >> vimage >> vnum ;
            InvertedFile[vword][vimage] = vnum;
        }
//        LOGI("ProgressCheck: InvertedFile[%d].size()= %d",vword,InvertedFile[vword].size());
        if( InvertedFile[ vword ].size() > 500 ){
            InvertedFile.erase( vword );
            prune_count++;
        }
    }
    InvertedIndexFile.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","InvertedIndexFile Loaded");
    LOGI("ProgressCheck: InvertedFile size= %d",(int)InvertedFile.size());
    LOGI("ProgressCheck: Prune count is %d",prune_count);

    //=======================================
    //	Loading the HKMeans Tree
    //=======================================
    String Hktree = str +"/BequestProto2/HKMeans_10000_10.Tree";
    ifstream t_file(Hktree.c_str(),ios::in);	//loading the tree
    LOGI("ProgressCheck: Tree file open status: %d", t_file.is_open());
    tree = ParseTree(t_file,0);				//parsing the tree
    t_file.close();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","HKMeans tree Loaded");

    //======================================================
    //	Loading Image file names and Term Frequency Count
    //======================================================
    string imageListFileName = str + "/BequestProto2/image_names.txt";

    string temp;
    ifstream imageListFile;
    imageListFile.open(imageListFileName.c_str(),ios::in);
    LOGI("ProgressCheck: Name list file open status: %d", imageListFile.is_open());
    string TF_filename = str + "/BequestProto2/descCount.txt";

    int tempTF;
    ifstream TF_file;
    TF_file.open(TF_filename.c_str(), ios::in );
    LOGI("ProgressCheck: Desc Count file open status: %d", TF_file.is_open());

    for(int i = 1 ; i <= N ; i++ ){
        imageListFile >> temp;
        ImageList.push_back( temp );		//all the image names go into ImageList
//        LOGI("ProgressCheck: Checking ImageList[i]: %s", ImageList[i-1].c_str() );
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
void Java_in_ac_iiit_cvit_bequest_JNiActivity_GetMatch(JNIEnv *env, jobject thiz, jlong inAddress,
                                                       jstring fileLocation) {

    /* PRE PROCESSING */
    QWords.clear();
    QueryHist.clear();
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Start...");

    string str;
    GetJStringContent(env,fileLocation,str);

    //str = str.append("/temp.jpg").c_str();
    str = str + "/pic.jpg";

    Mat img_temp = imread(str);

//    Mat &img_temp = *(Mat*) inAddress;

    if(img_temp.channels() == 3){
        cvtColor(img_temp,img_temp,CV_BGR2GRAY);
    }else{
        img_temp = img_temp.clone();
    }


	int height = img_temp.rows;
	int width = img_temp.cols;


    Mat img;
    if( height > 640 || width > 640 ){

        if(width >= height){
            int new_w = 640;
            int new_h = ((double)new_w*height)/width;
            Size size(new_w, new_h);
            resize(img_temp,img,size,0,0, INTER_LINEAR);
        }else{
            int new_h = 640;
            int new_w = ((double)new_h*width)/height;
            Size size(new_w, new_h);
            resize(img_temp,img,size,0,0, INTER_LINEAR);

        }

    }else{
        img = img_temp.clone();
    }

    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Frame Loaded");

    LOGI("ProgressCheck: Image Height = %d, Image Width = %d", img.rows,img.cols);

    SiftFeatureDetector detector;
//    std::vector<KeyPoint> keypoints;

    SiftDescriptorExtractor extractor;
    Mat descriptors;

    detector.detect(img,keypoints);
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","SIFT detection");

    extractor.compute(img,keypoints,descriptors);
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","SIFT descriptor extraction");

    int des[128];
	for(int i = 0 ; i < descriptors.rows ; i++ ){
        for(int j = 0 ; j < 128 ; j++){
            des[j] = descriptors.at<float>(i,j);
        }
        int path[12];
        int hist[12];
		hist[0] = 1;
		int depth = 0;
		GetPath(tree, des, path, depth, hist);
		for(int j = 0 ; j < 6 ; j++){
			if(path[j] == -1){
				depth = j;
				break;
			}
		}

		int finalWord = hist[depth];
		int n =depth;
		while(n){						//Uncomment only when Inverted index is made for leaf-1(node-1112) to leaf-10000(node-11111)
			finalWord = finalWord - pow(10,depth-n);	//Current inverted index is from node-1 to node-11111
			n--;
		}

//		depth = 4;
		if(QueryHist.count( finalWord ) > 0){
			QueryHist[ finalWord ] += 1;
		}
		else{
			QueryHist[ finalWord ] = 1;
//			cout << " ! " <<  hist[j] << endl;
		}
		QWords.push_back( finalWord );
    }
    LOGI("ProgressCheck: Total Query Words = %d", QWords.size() );
/* QUANTIZE ends */

    /* search starts*/


    double idf;
//    RImage *ImagesRetrieved = new RImage[N+5];
    ImagesRetrieved = new RImage[N+5];
    for(int i = 1 ; i <= N ; i++ ) {
        ImagesRetrieved[ i ].index  = i;
        ImagesRetrieved[ i ].r_score  = 0;
        ImagesRetrieved[ i ].m_score  = 0;
    }
    LOGI("ProgressCheck: Total images in the database (N) = %d", N );
    int word, count;
    for( map< int, int >::iterator it = QueryHist.begin() ; it != QueryHist.end() ; ++it){
        word = (*it).first;
        count = (*it).second;
        if( InvertedFile[ word ].size() == 0 ){
//            LOGI("InvertedFile[ %d ].size() = %d", word,InvertedFile[ word ].size() );
            continue;
        }

        idf = log10( N/InvertedFile[word].size() );
//        LOGI("ProgressCheck: Word = %d and idf = %f", word,idf );
        for( map< int , int >::iterator it2 = InvertedFile[word].begin() ; it2 != InvertedFile[word].end() ; ++it2 ){
//            LOGI("ProgressCheck: Entered for loop");
            int terms = TF[ (*it2).first-1 ] > QWords.size() ? TF[ (*it2).first-1 ] : QWords.size() ;
//            LOGI("ProgressCheck: terms = %f", terms );
            ImagesRetrieved[(*it2).first].r_score += ( ( ( count > (*it2).second ? (*it2).second : count ) * 1.0 )/(double)terms)  * idf;
            //			ImagesRetrieved[(*it2).first].r_score += count * idf;
//            LOGI("ProgressCheck: Image = %d, r_score = %f", ImagesRetrieved[(*it2).first].index,ImagesRetrieved[(*it2).first].r_score );
        }
    }

    qsort( ImagesRetrieved + 1, N , sizeof(ImagesRetrieved[ 1 ]), compare );

    for (int i=1;i<=10;i++){
        LOGI("ProgressCheck: Image number in Iindex search: %s and r_score = %f", ImageList[ImagesRetrieved[ i ].index - 1].c_str(),ImagesRetrieved[ i ].r_score );
    }
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Inverted index search is done");


    /* search ends*/
    /*Sorted Image list is stored in  ImagesRetrieved based on their r_score*/


}
}

extern "C"{
jstring JNICALL Java_in_ac_iiit_cvit_bequest_JNiActivity_GeoVerify(JNIEnv *env, jobject instance,
                                                                   jstring fileLocation){
    /*Goemetrical verification*/

    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Reached GeoVerify");

    string str;
    GetJStringContent(env,fileLocation,str);

    Mat RImg;
    std::vector<KeyPoint> Rkeypoints;
    Mat Rdescriptors;
    vector< int > RWords;
    string RImgName;
    vector < Mat > RMatches ;
    string WordKeyDir = str + "/BequestProto2/Words_n_Keys_10000_10/WordKey_";

    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Going to process 10 images");
    for(int n = 1 ;  n <= 10 ; n++){
//		RImgName = directoryPath + "1_img" + ImagesRetrieved[i].index + ".jpg";
        char RImgNumber[ 20 ];
        sprintf( RImgNumber, "%d", ImagesRetrieved[ n ].index );
        string RImgFileName(RImgNumber);
        string RImgFilePath = WordKeyDir + RImgFileName + ".txt";
        ifstream RImgFile( RImgFilePath.c_str(),ios::in );			//loading the text file with words and their corresponding keypoint locations of top 5 retrieved images
        cout<<RImgFilePath<<endl;
        int countWords, Rword;
        Point2f RPoint;
        RImgFile >> countWords;	//number of words
        for(int i = 0 ; i < countWords ; i++ ){
            RImgFile >> Rword >> RPoint.x >> RPoint.y;
            RWords.push_back( Rword );						//loading the words of the retrieved images
            KeyPoint temp(RPoint,0);
            Rkeypoints.push_back(temp);						//loading the keypoint locations of the retrieved images
        }


        std::vector< DMatch > imatches;

        for(int i = 0 ; i < RWords.size() ; i++ ){		//for each retreived image word check if it matches with any of the query image word
            for(int j = 0 ; j < QWords.size() ; j++ ){
                if( RWords[i] == QWords[j] ){
                    //			if(matches[i] == minc[j] && fabs(distMatchQ[i] - distMatchR[i]) < 10000){
                    DMatch tempDMatch;
                    tempDMatch.queryIdx = i;			//index of retrieved image word
                    tempDMatch.trainIdx = j;			//index of query word
                    tempDMatch.distance = 0;
                    imatches.push_back(tempDMatch);		//storing all the matches
                }
            }
        }
        ///////////////////////////
        ///////////////////////////

        std::vector<Point2f> obj;
        std::vector<Point2f> scene;

        for( int i = 0; i < imatches.size(); i++ )
        {
            //-- Get the keypoints from the good matches
            obj.push_back( Rkeypoints[ imatches[i].queryIdx ].pt );		//all the matching retrieved image words
            scene.push_back( keypoints[ imatches[i].trainIdx ].pt ); 	//all the matching query words
        }

        std::vector<uchar> inliers(obj.size(),0);
        if(obj.size() == 0 ) continue;
        Mat F = findFundamentalMat( obj, scene, inliers , CV_FM_RANSAC, 1, 0 );

        std::vector<cv::Point2f>::const_iterator itPts=obj.begin();	//iterator for retrieved image words
        std::vector<uchar>::const_iterator itIn= inliers.begin();	//iterator for inliers(outlier = 0, inlier = 1)
        std::vector<Point2f> obj_ransac;
        std::vector<Point2f> scene_ransac;



        int it = 0, total = 0;
        while (itPts!=obj.end()) {		//for all the retrieved words
            if (*itIn){			//if current word is an inlier then push corresponding keypoint locations
                obj_ransac.push_back( Rkeypoints[ imatches[ total ].queryIdx ].pt );	//keypoint location corresponding to inlier word from retrieved image
                scene_ransac.push_back( keypoints[ imatches[ total ].trainIdx ].pt );//keypoint location corresponding to inlier word from query image
                ++it;
            }
            ++itPts;
            ++itIn;
            total++;
        }

        ImagesRetrieved[ n ].m_score = 1.0 * it ;		//score of image based on inliers size

/*        if(it < 3 ) continue;	//if inliers are less than three then don't bother about object corners


		std::vector<Point2f> obj_corners(4);
		if(argc > 2 && argc == 6){
			obj_corners[0] = cvPoint( atof(argv[2]),atof(argv[3]) ); obj_corners[1] = cvPoint(  atof(argv[4]),atof(argv[3]) );
			obj_corners[2] = cvPoint(  atof(argv[4]),atof(argv[5]) ); obj_corners[3] = cvPoint(  atof(argv[2]),atof(argv[5]) );
		}else{
			obj_corners[0] = cvPoint(0,0); obj_corners[1] = cvPoint( RImg.cols, 0 );
			obj_corners[2] = cvPoint( RImg.cols, RImg.rows ); obj_corners[3] = cvPoint( 0, RImg.rows );
		}
		(ImagesRetrieved[ n ].corners).reserve(4);		//reserve some memory for corner locations

		Mat H = findHomography( obj_ransac, scene_ransac, CV_RANSAC );		//find homography matrix based on two keypoint sets related to  final sets of words
		perspectiveTransform( obj_corners, ImagesRetrieved[ n ].corners, H );	//getting perspective transform based on Homography matrix

*/

        Rkeypoints.erase(Rkeypoints.begin(), Rkeypoints.end());	//erase everything to make way for next retrieved image
        RWords.erase(RWords.begin(), RWords.end());
        Rdescriptors.release();

    }

    qsort( ImagesRetrieved + 1, 10 , sizeof(ImagesRetrieved[ 1 ]), compare2 );	//sort retrieved images based on their m_score
    __android_log_write(ANDROID_LOG_VERBOSE,"Progress","Processed and sorted 10 images");
    /*Goemetrical verification done*/


    keypoints.erase(keypoints.begin(),keypoints.end());

    for (int i=1;i<=10;i++){
        LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ i ].index  - 1 ].c_str(),ImagesRetrieved[ i ].m_score );
    }
/**
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 1 ].index  - 1 ].c_str(),ImagesRetrieved[ 1 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 2 ].index  - 1 ].c_str(),ImagesRetrieved[ 2 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 3 ].index  - 1 ].c_str(),ImagesRetrieved[ 3 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 4 ].index  - 1 ].c_str(),ImagesRetrieved[ 4 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 5 ].index  - 1 ].c_str(),ImagesRetrieved[ 5 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 6 ].index  - 1 ].c_str(),ImagesRetrieved[ 6 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 7 ].index  - 1 ].c_str(),ImagesRetrieved[ 7 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 8 ].index  - 1 ].c_str(),ImagesRetrieved[ 8 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 9 ].index  - 1 ].c_str(),ImagesRetrieved[ 9 ].m_score);
    LOGI("ProgressCheck: Image : %s, m_score = %f", ImageList[ImagesRetrieved[ 10 ].index  - 1].c_str(),ImagesRetrieved[ 10 ].m_score);
**/

    string Top;
    //set the number in the if case after testing it out on printouts
    if(ImagesRetrieved[ 1 ].m_score > 3){
        Top = ImageList[ ImagesRetrieved[ 1 ].index - 1 ];
        stringstream ss;
        ss << ImagesRetrieved[ 1 ].m_score;
        String imgInliers = ss.str();
        Top = Top + "_"+imgInliers;
        stringstream st;
        st << ImagesRetrieved[ 1 ].r_score;
        LOGI("ProgressCheck: Image : %s, r_score = %f", ImageList[ImagesRetrieved[ 1 ].index  - 1].c_str(),ImagesRetrieved[ 1 ].r_score);
        String imgR_score = st.str();
        Top = Top + "_"+imgR_score;

    }	//Top retrieved image
    else{
        Top = "";
    }
//    stringstream ss;
//    ss << ImagesRetrieved[1].index;
//    String imgNum = ss.str();

//    string Top = "1_img" + imgNum + ".jpg";
    __android_log_write(ANDROID_LOG_VERBOSE,"Top Retrieved Image is ",Top.c_str());


    return env->NewStringUTF(Top.c_str());


}
}

extern "C"
jstring Java_in_ac_iiit_cvit_bequest_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Welcome";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
{
void JNICALL Java_in_ac_iiit_cvit_bequest_CameraActivity_salt(JNIEnv *env, jobject instance,
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

