//------------------------------------------------------------------------------
//
//	Copyright (C) 2014 Nexell Co. All Rights Reserved
//	Nexell Co. Proprietary & Confidential
//
//	NEXELL INFORMS THAT THIS CODE AND INFORMATION IS PROVIDED "AS IS" BASE
//  AND	WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING
//  BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
//  FOR A PARTICULAR PURPOSE.
//
//	Module		:
//	File		:
//	Description	:
//	Author		: 
//	Export		:
//	History		:
//
//------------------------------------------------------------------------------

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <pthread.h>

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <NX_MoviePlay.h>

#define LOG_TAG	"libnxmovieplayer"

#if(0)
#define LOGV(...)   __android_log_print(ANDROID_LOG_VERBOSE,	LOG_TAG, __VA_ARGS__)
#define LOGD(...)   __android_log_print(ANDROID_LOG_DEBUG,		LOG_TAG, __VA_ARGS__)
#define LOGI(...)   __android_log_print(ANDROID_LOG_INFO,		LOG_TAG, __VA_ARGS__)
#define LOGW(...)   __android_log_print(ANDROID_LOG_WARN,		LOG_TAG, __VA_ARGS__)
#define LOGE(...)   __android_log_print(ANDROID_LOG_ERROR,		LOG_TAG, __VA_ARGS__)
#else
#define LOGV(...)
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN,		LOG_TAG, __VA_ARGS__)
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR,		LOG_TAG, __VA_ARGS__)
#endif

////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	Fucntion Lock
//
class CNX_AutoLock {
public:
    CNX_AutoLock( pthread_mutex_t *pLock ) : m_pLock(pLock) {
        pthread_mutex_lock( m_pLock );
    }
    ~CNX_AutoLock() {
        pthread_mutex_unlock(m_pLock);
    }

protected:
    pthread_mutex_t *m_pLock;

private:
    CNX_AutoLock (const CNX_AutoLock &Ref);
    CNX_AutoLock &operator=(CNX_AutoLock &Ref);
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	Global Variable
//
static JavaVM 		*gJavaVM;
static jclass 		gClass;
static jmethodID 	gMethodID;

MP_HANDLE	hMoviePlayer = 0x00;
Media_Info	gMediaInfo;

char 		gUriBuf[256];

int			gAudioRequestTrackNum = 1;
int			bIsPlay = false;

ANativeWindow		*pNativeWindow1 = NULL;
ANativeWindow		*pNativeWindow2 = NULL;

pthread_mutex_t		hLock;

////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	Interface Function
//
#define VIDEO_TYPE_NUM	20
static const char *VideoTypeString[VIDEO_TYPE_NUM] = {
	"CODEC_H264",		"CODEC_H263",		"CODEC_MPEG1VIDEO",	"CODEC_MPEG2VIDEO",	"CODEC_MPEG4",
	"CODEC_MSMPEG4V3",	"CODEC_FLV1",		"CODEC_WMV1",		"CODEC_WMV2",		"CODEC_WMV3",
	"CODEC_VC1",		"CODEC_RV30",		"CODEC_RV40",		"CODEC_THEORA",		"CODEC_VP8",
	"RESERVED"			"RESERVED",			"RESERVED",			"RESERVED",			"RESERVED"
};

#define AUDIO_TYPE_NUM	20
static const char *AudioTypeString[AUDIO_TYPE_NUM] = {
	"CODEC_RA_144",		"CODEC_RA_288",		"CODEC_MP2",		"CODEC_MP3",		"CODEC_AAC",
	"CODEC_AC3",		"CODEC_DTS",		"CODEC_VORBIS",		"CODEC_WMAV1",		"CODEC_WMAV2",
	"CODEC_WMAPRO",		"CODEC_FLAC",		"CODEC_COOK",		"CODEC_APE",		"CODEC_AAC_LATM",
	"CODEC_PCM_S16LE",	"RESERVED",			"RESERVED",			"RESERVED",			"RESERVED"
};

static void MediaStreamInfo()
{
	if( gMediaInfo.VideoTrackTotNum > 0 ) {
		LOGI("-------------------------------------------------------------------------------");
		LOGI("                         Video Information                                     ");
		LOGI("-------------------------------------------------------------------------------");
		LOGI("    VideoTrackTotNum : %d", (int)gMediaInfo.VideoTrackTotNum);

		for( int i = 0; i < (int)gMediaInfo.VideoTrackTotNum; i++ )
		{
			LOGI("    VideoTrackNum    : %d    ", (int)gMediaInfo.VideoInfo[i].VideoTrackNum + 1);
			LOGI("    VCodecType       : %d, %s", (int)gMediaInfo.VideoInfo[i].VCodecID, VideoTypeString[gMediaInfo.VideoInfo[i].VCodecID - 1]);
			LOGI("    Width            : %d    ", (int)gMediaInfo.VideoInfo[i].Width);
			LOGI("    Height           : %d    ", (int)gMediaInfo.VideoInfo[i].Height);
		}
	}

	if( gMediaInfo.AudioTrackTotNum > 0 ) {
		LOGI("-------------------------------------------------------------------------------");
		LOGI("                         Audio Information                                     ");
		LOGI("-------------------------------------------------------------------------------");
		LOGI("    AudioTrackTotNum : %d", (int)gMediaInfo.AudioTrackTotNum);


		for( int i = 0; i < (int)gMediaInfo.AudioTrackTotNum; i++ )
		{
			LOGI("    AudioTrackNum    : %d    ", (int)gMediaInfo.AudioInfo[i].AudioTrackNum + 1);
			LOGI("    ACodecType       : %d, %s", (int)gMediaInfo.AudioInfo[i].ACodecID, AudioTypeString[gMediaInfo.AudioInfo[i].ACodecID - 0x1001]);
			LOGI("    samplerate       : %d    ", (int)gMediaInfo.AudioInfo[i].samplerate);
			LOGI("    channels         : %d    ", (int)gMediaInfo.AudioInfo[i].channels);
		}
	}
	if( gMediaInfo.DataTrackTotNum > 0 )
	{
		LOGI("-------------------------------------------------------------------------------");
		LOGI("                         Data Information                                      ");
		LOGI("-------------------------------------------------------------------------------");
		LOGI("    DataTrackTotNum  : %d", (int)gMediaInfo.DataTrackTotNum);
	}

	if( (gMediaInfo.VideoTrackTotNum == 0) && (gMediaInfo.AudioTrackTotNum == 0) && (gMediaInfo.DataTrackTotNum == 0) )
	{
		LOGI("    Warring! Invalid Media Stream Information (VideoTrackTotNum = %d, AudioTrackTotNum = %d, DataTrackTotNum = %d)",
			gMediaInfo.VideoTrackTotNum, gMediaInfo.AudioTrackTotNum, gMediaInfo.DataTrackTotNum);
	}
}

static void EventCallback(void *privateDesc, unsigned int EventType, unsigned int EventData, unsigned int param2)
{
	static char __FUNC__[32] = "EventCallback";
	LOGV("%s()++", __FUNC__);

	JNIEnv *env;
	if( JNI_OK != gJavaVM->AttachCurrentThread( &env, NULL ) ) {
		LOGE("%s(): AttachCurrentThread() failed.", __FUNC__);
		return;
	}

	if( gClass && gMethodID ) {
		env->CallStaticVoidMethod( gClass, gMethodID, (int)EventType, (int)EventData );	
	}
	else {
		LOGE("%s(): CallStaticVoidMethod() failed. - gClass( 0x%08x ), gMethodID( 0x%08x ).", __FUNC__, (unsigned int)gClass, (unsigned int)gMethodID );
	}
	

	if( JNI_OK != gJavaVM->DetachCurrentThread() ) {
		LOGE("%s(): DetachCurrentThread() failed.", __FUNC__);
	}

	LOGV("%s()--", __FUNC__);
}

JNIEXPORT void JNICALL Mp_JniInit( JNIEnv *env, jclass obj )
{
	static char __FUNC__[32] = "Mp_JniInit";
	LOGV("%s()++", __FUNC__);
	env->GetJavaVM( &gJavaVM );

	if( !(gClass = env->FindClass( "com/example/nxplayerbasedfilter/PlayerActivity" )) ) {
		LOGE("%s(): FindClass() failed.\n", __FUNC__);
		return;
	}

	if( !(gMethodID = env->GetStaticMethodID( gClass, "EventHandler", "(II)V" )) ) {
		LOGE("%s(): GetStaticMethodID() failed.\n", __FUNC__);
	}

	pthread_mutex_init( &hLock, NULL );

	LOGV("%s()--", __FUNC__);
}

JNIEXPORT void JNICALL Mp_JniDeinit( JNIEnv *env, jclass obj )
{
	static char __FUNC__[32] = "Mp_JniDeinit";
	LOGV("%s()++", __FUNC__);
	
	pthread_mutex_destroy( &hLock );
	
	LOGV("%s()--", __FUNC__);	
}

JNIEXPORT jint JNICALL Mp_SetFileName( JNIEnv *env, jclass obj, jstring uri )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_SetFileName";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer != NULL ) {
		LOGE("%s(): Error! Handle is already initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	const char *pBuf = env->GetStringUTFChars(uri, 0);
	strcpy(gUriBuf, pBuf);
	env->ReleaseStringUTFChars(uri, pBuf);

	MP_RESULT mpResult = NX_MPSetFileName( &hMoviePlayer, gUriBuf, &gMediaInfo );
	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPSetFileName() Failed! (ret = %d, uri = %s)", __FUNC__, mpResult, gUriBuf);
	}

	if( gMediaInfo.AudioTrackTotNum != 0 ) 	gAudioRequestTrackNum = 1;
	else									gAudioRequestTrackNum = 0;

	MediaStreamInfo();

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jstring JNICALL Mp_GetMediaInfo( JNIEnv *env, jclass obj )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_GetMediaInfo";
	LOGV("%s()++", __FUNC__);

	char destBuf[1024], tempBuf[128];
	memset(destBuf, 0x00, sizeof(destBuf));

	if( hMoviePlayer == NULL ) {
		LOGE("%s: Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return env->NewStringUTF(destBuf);
	}

	sprintf(tempBuf, "Media FileName\n: %s\n\n", gUriBuf);
	strcat( destBuf, tempBuf );

	if( gMediaInfo.VideoTrackTotNum > 0 ) {
		sprintf( tempBuf, "Video Infomation\n" );
		strcat( destBuf, tempBuf );

		for( int i = 0; i < (int)gMediaInfo.VideoTrackTotNum; i++ ) {
			sprintf( tempBuf, " Video Track #%d\n", (int)gMediaInfo.VideoInfo[i].VideoTrackNum + 1 );
			strcat( destBuf, tempBuf );

			sprintf( tempBuf, " -. Codec Type    : %s\n", VideoTypeString[gMediaInfo.VideoInfo[i].VCodecID - 1] );
			strcat( destBuf, tempBuf );

			sprintf( tempBuf, " -. Resolution    : %d x %d\n\n", (int)gMediaInfo.VideoInfo[i].Width, (int)gMediaInfo.VideoInfo[i].Height );
			strcat( destBuf, tempBuf );
		}
	}

	if( gMediaInfo.AudioTrackTotNum > 0 ) {
		sprintf( tempBuf, "Audio Infomation\n" );
		strcat( destBuf, tempBuf );

		for( int i = 0; i < (int)gMediaInfo.AudioTrackTotNum; i++ ) {
			sprintf( tempBuf, " Audio Track #%d\n", (int)gMediaInfo.AudioInfo[i].AudioTrackNum + 1 );
			strcat( destBuf, tempBuf );

			sprintf( tempBuf, " -. Codec Type    : %s\n", AudioTypeString[gMediaInfo.AudioInfo[i].ACodecID - 0x1001]);
			strcat( destBuf, tempBuf );

			sprintf( tempBuf, " -. Sampling Rate : %d\n", (int)gMediaInfo.AudioInfo[i].samplerate );
			strcat( destBuf, tempBuf );

			sprintf( tempBuf, " -. Channels      : %d\n\n", (int)gMediaInfo.AudioInfo[i].channels );
			strcat( destBuf, tempBuf );
		}
	}

	if( gMediaInfo.DataTrackTotNum > 0 ) {
		sprintf( tempBuf, "Valid Data Track\n" );
		strcat( destBuf, tempBuf );
	}

	LOGV("%s()--", __FUNC__);
	return env->NewStringUTF(destBuf);
}

JNIEXPORT jint JNICALL Mp_GetVideoTrackNum( JNIEnv *env, jclass obj )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_GetVideoNumber";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	LOGV("%s()--", __FUNC__);
	return gMediaInfo.VideoTrackTotNum;
}

//JNIEXPORT jint JNICALL Mp_GetVideoWidth( JNIEnv *env, jclass obj, int vidRequest )
//{
//	CNX_AutoLock lock( &hLock );
//
//	static char __FUNC__[32] = "Mp_GetVideoWidth";
//	LOGV("%s()++", __FUNC__);
//
//	if( hMoviePlayer == NULL ) {
//		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
//		LOGV("%s()--", __FUNC__);
//		return -1;
//	}
//
//	if( vidRequest <= 0 || vidRequest > gMediaInfo.VideoTrackTotNum ) {
//		LOGE("%s(): Error! Illegal VideoRequestNumber(%d)!", __FUNC__, vidRequest);
//		LOGV("%s()--", __FUNC__);
//		return -1;
//	}
//
//	LOGV("%s()--", __FUNC__);
//	return gMediaInfo.VideoInfo[vidRequest-1].Width;
//}
//
//JNIEXPORT jint JNICALL Mp_GetVideoHeight( JNIEnv *env, jclass obj, int vidRequest )
//{
//	CNX_AutoLock lock( &hLock );
//
//	static char __FUNC__[32] = "Mp_GetVideoHeight";
//	LOGV("%s()++", __FUNC__);
//
//	if( hMoviePlayer == NULL ) {
//		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
//		LOGV("%s()--", __FUNC__);
//		return -1;
//	}
//
//	if( vidRequest <= 0 || vidRequest > gMediaInfo.VideoTrackTotNum ) {
//		LOGE("%s(): Error! Illegal VideoRequestNumber(%d)!", __FUNC__, vidRequest);
//		LOGV("%s()--", __FUNC__);
//		return -1;
//	}
//
//	LOGV("%s()--", __FUNC__);
//	return gMediaInfo.VideoInfo[vidRequest-1].Height;
//}

JNIEXPORT jint JNICALL Mp_Open( JNIEnv *env, jclass obj, jobject jSurface1, jobject jSurface2, int vidRequest, jboolean pipOn )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Open";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	if( jSurface1 != NULL )	pNativeWindow1 = ANativeWindow_fromSurface( env, jSurface1 );
	if( jSurface2 != NULL ) pNativeWindow2 = ANativeWindow_fromSurface( env, jSurface2 );

	LOGI("pNativeWindow1(%p), pNativeWindow(%p), videoChannel(%d), pipOn(%d)", pNativeWindow1, pNativeWindow2, vidRequest, pipOn);

	MP_RESULT mpResult = NX_MPOpen( hMoviePlayer, gAudioRequestTrackNum, vidRequest, (int)pipOn, (void*)pNativeWindow1, (void*)pNativeWindow2, NULL, &EventCallback, (void*)hMoviePlayer );
	if( ERROR_NONE != mpResult ) {
 		LOGE("%s(): Error! NX_MPOpen() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jint JNICALL Mp_Close(JNIEnv *env, jclass obj)
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Close";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s: Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	// Media Player Close
	NX_MPClose( hMoviePlayer );
	hMoviePlayer = NULL;

	// Native Window Release
	if( pNativeWindow1 ) {
		ANativeWindow_release( pNativeWindow1 );
		pNativeWindow1 = NULL;
	}

	if( pNativeWindow2 ) {
		ANativeWindow_release( pNativeWindow2 );
		pNativeWindow2 = NULL;
	}

	LOGV("%s()--", __FUNC__);
	return 0;
}

JNIEXPORT jint JNICALL Mp_Play(JNIEnv *env, jclass obj)
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Play";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s: Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	MP_RESULT mpResult = NX_MPPlay( hMoviePlayer, 1.0 );
	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPPlay() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	bIsPlay = true;

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jint JNICALL Mp_Pause(JNIEnv *env, jclass obj)
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Pause";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s: Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	MP_RESULT mpResult = NX_MPPause( hMoviePlayer );
	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPPause() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	bIsPlay = false;

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jint JNICALL Mp_Stop(JNIEnv *env, jclass obj)
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Stop";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s: Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	MP_RESULT mpResult = NX_MPStop( hMoviePlayer );
	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPStop() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	bIsPlay = false;

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jint JNICALL Mp_IsPlay( JNIEnv *env, jclass obj )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_IsPlay";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	LOGV("%s()--", __FUNC__);
	return bIsPlay;
}

JNIEXPORT jint JNICALL Mp_Seek( JNIEnv *env, jclass obj, jint seekTime )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_Seek";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	MP_RESULT mpResult = NX_MPSeek( hMoviePlayer, seekTime );
	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPSeek() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	LOGV("%s()--", __FUNC__);
	return mpResult;
}

JNIEXPORT jint JNICALL Mp_GetCurDuration( JNIEnv *env, jclass obj )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_GetCurDuration";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	unsigned int duration = 0;
	MP_RESULT mpResult = NX_MPGetCurDuration( hMoviePlayer, &duration );

	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPGetCurDuration() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	LOGV("%s()--", __FUNC__);
	return (jint)duration;
}

JNIEXPORT jint JNICALL Mp_GetCurPosition( JNIEnv *env, jclass obj )
{
	CNX_AutoLock lock( &hLock );

	static char __FUNC__[32] = "Mp_GetCurPosition";
	LOGV("%s()++", __FUNC__);

	if( hMoviePlayer == NULL ) {
		LOGE("%s(): Error! Handle is not initialized!", __FUNC__);
		LOGV("%s()--", __FUNC__);
		return -1;
	}

	unsigned int position = 0;
	MP_RESULT mpResult = NX_MPGetCurPosition( hMoviePlayer, &position );

	if( ERROR_NONE != mpResult ) {
		LOGE("%s(): Error! NX_MPGetCurPosition() Failed! (ret = %d)", __FUNC__, mpResult);
	}

	LOGV("%s()--", __FUNC__);
	return (jint)position;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	Implementation JNI_OnLoad()
//
static JNINativeMethod sMethods[] = {
	//	Native Function Name,		Sigunature, 				C++ Function Name
	{ "Mp_JniInit",				"()V",						(void*)Mp_JniInit },
	{ "Mp_JniDeinit", 			"()V",						(void*)Mp_JniDeinit },
	{ "Mp_SetFileName",			"(Ljava/lang/String;)I",	(void*)Mp_SetFileName },
	{ "Mp_GetMediaInfo",		"()Ljava/lang/String;",		(void*)Mp_GetMediaInfo },
	{ "Mp_GetVideoTrackNum",	"()I",						(void*)Mp_GetVideoTrackNum },
//	{ "Mp_GetVideoWidth",		"(I)I",						(void*)Mp_GetVideoWidth },
//	{ "Mp_GetVideoHeight",		"(I)I",						(void*)Mp_GetVideoHeight },
	{ "Mp_Open", 				"(Landroid/view/Surface;Landroid/view/Surface;IZ)I",	(void*)Mp_Open },
	{ "Mp_Close", 				"()I",						(void*)Mp_Close },
	{ "Mp_Play", 				"()I",						(void*)Mp_Play },
	{ "Mp_Pause", 				"()I",						(void*)Mp_Pause },
	{ "Mp_Stop", 				"()I",						(void*)Mp_Stop },
	{ "Mp_IsPlay",				"()I",						(void*)Mp_IsPlay },
	{ "Mp_Seek", 				"(I)I",						(void*)Mp_Seek },
	{ "Mp_GetCurDuration", 		"()I",						(void*)Mp_GetCurDuration },
	{ "Mp_GetCurPosition", 		"()I",						(void*)Mp_GetCurPosition },
};

static int RegisterNativeMethods( JNIEnv *env, const char *className, JNINativeMethod *gMethods, int numMethods )
{
	static char __FUNC__[32] = "RegisterNativeMethods";
	LOGV("%s()++", __FUNC__);

	jclass clazz;
	int result = JNI_FALSE;

	clazz = env->FindClass( className );
	if( clazz == NULL ) {
		LOGE("%s(): Native registeration unable to find class '%s'\n", __FUNC__, className);
		goto FAIL;
	}

	if( env->RegisterNatives( clazz, gMethods, numMethods) < 0 ) {
		LOGE("%s(): RegisterNatives failed for '%s'\n", __FUNC__, className);
		goto FAIL;
	}

	result = JNI_TRUE;

FAIL:
	LOGV("%s()--", __FUNC__);
	return result;
}

jint JNI_OnLoad( JavaVM *vm, void *reserved )
{
	static char __FUNC__[32] = "JNI_OnLoad";
	LOGV("%s()++", __FUNC__);

	jint result = -1;
	JNIEnv *env = NULL;

	if( vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK ) {
		LOGE("%s(): GetEnv failed!\n", __FUNC__);
		goto FAIL;
	}

	if( RegisterNativeMethods(env, "com/example/nxplayerbasedfilter/MoviePlayer", sMethods, sizeof(sMethods) / sizeof(sMethods[0]) ) != JNI_TRUE ) {
		LOGE("%s(): RegisterNativeMethods failed!\n", __FUNC__);
		goto FAIL;
	}

	result = JNI_VERSION_1_4;

FAIL:
	LOGV("%s()--", __FUNC__);
	return result;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
	static char __FUNC__[32] = "JNI_OnUnload";
	LOGV("%s()++", __FUNC__);

	JNIEnv *env = NULL;

	if( vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK ) {
		LOGE("%s(): GetEnv failed!\n", __FUNC__);
		goto FAIL;
	}

FAIL:
	LOGV("%s()--", __FUNC__);
}
