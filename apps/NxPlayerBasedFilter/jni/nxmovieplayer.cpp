// THIS IS FITLER NEW VERSION

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

#define NX_DTAG		"libnxmovieplayer"

//------------------------------------------------------------------------------
//
//	Debug Tools
//
#define NX_DBG_VBS			2	// ANDROID_LOG_VERBOSE
#define NX_DBG_DEBUG		3	// ANDROID_LOG_DEBUG
#define	NX_DBG_INFO			4	// ANDROID_LOG_INFO
#define	NX_DBG_WARN			5	// ANDROID_LOG_WARN
#define	NX_DBG_ERR			6	// ANDROID_LOG_ERROR
#define NX_DBG_DISABLE		9

int gNxFilterDebugLevel 	= NX_DBG_INFO;

#define DBG_PRINT			__android_log_print
#define NxTrace(...)		DBG_PRINT(ANDROID_LOG_VERBOSE, NX_DTAG, __VA_ARGS__);

#define NxDbgMsg(A, ...)	do {										\
								if( gNxFilterDebugLevel <= A ) {		\
									DBG_PRINT(A, NX_DTAG, __VA_ARGS__);	\
								}										\
							} while(0)


//------------------------------------------------------------------------------
//
//	Fucntion Lock
//
class CNX_AutoLock {
public:
    CNX_AutoLock( pthread_mutex_t *pLock )
    	: m_pLock(pLock)
    {
        pthread_mutex_lock( m_pLock );
    }
    ~CNX_AutoLock()
    {
        pthread_mutex_unlock( m_pLock );
    }

protected:
    pthread_mutex_t *m_pLock;

private:
    CNX_AutoLock (const CNX_AutoLock &Ref);
    CNX_AutoLock &operator=(CNX_AutoLock &Ref);
};


//------------------------------------------------------------------------------
//
//	Global Variable
//
#define MAX_DISPLAY_CHANNEL		8

static JavaVM 		*gJavaVM;
static jclass 		gClass;
static jmethodID 	gMethodID;

char				gClassName[1024];
char				gCallbackName[1024];

MP_HANDLE			hMoviePlayer = NULL;
MP_MEDIA_INFO		gMediaInfo;
char 				gUriBuf[256];

ANativeWindow		*gpNativeWindow[MAX_DISPLAY_CHANNEL];
MP_DSP_CONFIG		*gpDspConfig[MAX_DISPLAY_CHANNEL];

bool				bPlay	= false;
bool				bPause	= false;
pthread_mutex_t		hLock;


//------------------------------------------------------------------------------
//
//	Interface Function
//

//------------------------------------------------------------------------------
typedef struct CodecType {
	int32_t CodecId;
	char 	CodecString[16];
} CodecType;

static CodecType gCodecType[] = {
	{     1, "MPEG1VIDEO"	},	{     2, "MPEG2VIDEO"	},	{     5, "H263"			},	{    13, "MPEG4"		},
	{    17, "MSMPEG4V3"	},	{    20, "H263P"		},	{    21, "H263I"		},	{    22, "FLV1"			},
	{    28, "H264"			},	{    31, "THEORA"		},	{    69, "RV30"			},	{    70, "RV40"			},
	{    71, "VC1"			},	{    72, "WMV3"			},	{   141, "VP8"			},

	{ 77824, "RA_144"		},	{ 77825, "RA_288"		},	{ 86016, "MP2"			},	{ 86017, "MP3"			},
	{ 86018, "AAC"			},	{ 86019, "AC3"			},	{ 86020, "DTS"			},	{ 86021, "VORBIS"		},
	{ 86023, "WMAV1"		},	{ 86024, "WMAV2"		},	{ 86028, "FLAC"			},	{ 86036, "COOK"			},
	{ 86048, "APE"			},	{ 86053, "WMAPRO"		},	{ 86065, "AAC_LATM"		},
	{ 65536, "PCM"			},	{ 65537, "PCM"			},	{ 65538, "PCM"			},	{ 65539, "PCM"			},
	{ 65540, "PCM"			},	{ 65541, "PCM"			},	{ 65542, "PCM"			},	{ 65543, "PCM"			},
	{ 65544, "PCM"			},	{ 65545, "PCM"			},	{ 65546, "PCM"			},	{ 65547, "PCM"			},
	{ 65548, "PCM"			},	{ 65549, "PCM"			},	{ 65550, "PCM"			},	{ 65551, "PCM"			},
	{ 65552, "PCM"			},	{ 65553, "PCM"			},	{ 65554, "PCM"			},	{ 65555, "PCM"			},
	{ 65556, "PCM"			},	{ 65557, "PCM"			},	{ 65558, "PCM"			},	{ 65559, "PCM"			},
	{ 65560, "PCM"			},	{ 65561, "PCM"			},	{ 65562, "PCM"			},
	{ 69632, "ADPCM"		},	{ 69633, "ADPCM"		},	{ 69634, "ADPCM"		},	{ 69635, "ADPCM"		},
	{ 69636, "ADPCM"		},	{ 69637, "ADPCM"		},	{ 69638, "ADPCM"		},	{ 69639, "ADPCM"		},
	{ 69640, "ADPCM"		},	{ 69641, "ADPCM"		},	{ 69642, "ADPCM"		},	{ 69643, "ADPCM"		},
	{ 69644, "ADPCM"		},	{ 69645, "ADPCM"		},	{ 69646, "ADPCM"		},	{ 69647, "ADPCM"		},
	{ 69648, "ADPCM"		},	{ 69649, "ADPCM"		},	{ 69650, "ADPCM"		},	{ 69651, "ADPCM"		},
	{ 69652, "ADPCM"		},	{ 69653, "ADPCM"		},	{ 69654, "ADPCM"		},	{ 69655, "ADPCM"		},
	{ 69656, "ADPCM"		},	{ 69657, "ADPCM"		},	{ 69658, "ADPCM"		},	{ 69659, "ADPCM"		},
	{ 69660, "ADPCM"		},
};

//------------------------------------------------------------------------------
static void PrintMediaInfo( void )
{
	int pos = 0;
	NxDbgMsg( NX_DBG_INFO, "FileName : %s\n\n", gUriBuf );

	NxDbgMsg( NX_DBG_INFO, "iProgramNum       : %d\n", gMediaInfo.iProgramNum );
	NxDbgMsg( NX_DBG_INFO, "iAudioTrackNum    : %d\n", gMediaInfo.iAudioTrackNum );
	NxDbgMsg( NX_DBG_INFO, "iVideoTrackNum    : %d\n", gMediaInfo.iVideoTrackNum );
	NxDbgMsg( NX_DBG_INFO, "iSubTitleTrackNum : %d\n", gMediaInfo.iSubTitleTrackNum );
	NxDbgMsg( NX_DBG_INFO, "iDataTrackNum     : %d\n", gMediaInfo.iDataTrackNum );

	for( int32_t i = 0; i < gMediaInfo.iProgramNum; i++ )
	{
		NxDbgMsg( NX_DBG_INFO, "********** Program #%d **********\n", i );

		NxDbgMsg( NX_DBG_INFO, "iAudioNum    : %d\n", gMediaInfo.ProgramInfo[i].iAudioNum);
		NxDbgMsg( NX_DBG_INFO, "iVideoNum    : %d\n", gMediaInfo.ProgramInfo[i].iVideoNum);
		NxDbgMsg( NX_DBG_INFO, "iSubTitleNum : %d\n", gMediaInfo.ProgramInfo[i].iSubTitleNum);
		NxDbgMsg( NX_DBG_INFO, "iDataNum     : %d\n", gMediaInfo.ProgramInfo[i].iDataNum);
		NxDbgMsg( NX_DBG_INFO, "iDuration    : %lld\n", gMediaInfo.ProgramInfo[i].iDuration);

		if( 0 < gMediaInfo.ProgramInfo[i].iVideoNum )
		{
			int num = 0;
			NxDbgMsg( NX_DBG_INFO, "[ Video Information ]\n" );

			for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
			{
				MP_TRACK_INFO *pTrackInfo = &gMediaInfo.ProgramInfo[i].TrackInfo[j];
				
				if( MP_TRACK_VIDEO == pTrackInfo->iTrackType ) {
					NxDbgMsg( NX_DBG_INFO, " Video Track #%d\n", num++ );
					NxDbgMsg( NX_DBG_INFO, "  -. Track Index : %d\n", pTrackInfo->iTrackIndex );
					NxDbgMsg( NX_DBG_INFO, "  -. Codec Type  : %d\n", (int)pTrackInfo->iCodecId );
					NxDbgMsg( NX_DBG_INFO, "  -. Resolution  : %d x %d\n", pTrackInfo->iWidth, pTrackInfo->iHeight );
					if( 0 > pTrackInfo->iDuration )
						NxDbgMsg( NX_DBG_INFO, "  -. Duration    : Unknown\n\n", pTrackInfo->iDuration );
					else
						NxDbgMsg( NX_DBG_INFO, "  -. Duration    : %lld ms\n\n", pTrackInfo->iDuration );
				}
			}
		}

		if( 0 < gMediaInfo.ProgramInfo[i].iAudioNum )
		{
			int num = 0;
			NxDbgMsg( NX_DBG_INFO, "[ Audio Information ]\n" );

			for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
			{
				MP_TRACK_INFO *pTrackInfo = &gMediaInfo.ProgramInfo[i].TrackInfo[j];

				if( MP_TRACK_AUDIO == pTrackInfo->iTrackType ) {
					NxDbgMsg( NX_DBG_INFO, " Audio Track #%d\n", num++ );
					NxDbgMsg( NX_DBG_INFO, "  -. Track Index : %d\n", pTrackInfo->iTrackIndex );
					NxDbgMsg( NX_DBG_INFO, "  -. Codec Type  : %d\n", (int)pTrackInfo->iCodecId );
					NxDbgMsg( NX_DBG_INFO, "  -. Channels    : %d\n", pTrackInfo->iChannels );
					NxDbgMsg( NX_DBG_INFO, "  -. SampleRate  : %d Hz\n", pTrackInfo->iSampleRate );
					NxDbgMsg( NX_DBG_INFO, "  -. Bitrate     : %d bps\n", pTrackInfo->iBitrate );
					if( 0 > pTrackInfo->iDuration )
						NxDbgMsg( NX_DBG_INFO, "  -. Duration    : Unknown\n\n", pTrackInfo->iDuration );
					else
						NxDbgMsg( NX_DBG_INFO, "  -. Duration    : %lld ms\n\n", pTrackInfo->iDuration );
				}					
			}
		}
	}
}

//------------------------------------------------------------------------------
static void EventCallback( void *privateDesc, unsigned int EventType, unsigned int EventData, unsigned int param2 )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );

	JNIEnv *env;
	if( JNI_OK != gJavaVM->AttachCurrentThread( &env, NULL ) ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): AttachCurrentThread() failed.", __FUNCTION__ );
		return;
	}

	if( gClass && gMethodID ) {
		env->CallStaticVoidMethod( gClass, gMethodID, (int)EventType, (int)EventData );	
	}
	else {
		NxDbgMsg( NX_DBG_ERR, "%s(): CallStaticVoidMethod() failed. - gClass( 0x%08x ), gMethodID( 0x%08x ).", __FUNCTION__, (unsigned int)gClass, (unsigned int)gMethodID );
	}

	if( JNI_OK != gJavaVM->DetachCurrentThread() ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): DetachCurrentThread() failed.", __FUNCTION__);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}

// Since android 4.0 garbage collector was changed. Now it moves object around during garbage collection,
// which can cause a lot of problems. Imagine that you have a static variable pointing to an object,
// and then this object gets moved by gc. Since android uses direct pointers for java objects,
// this would mean that your static variable is now pointing to a random address in the memory,
// unoccupied by any object or occupied by an object of different sort.
// This will almost guarantee that you'll get EXC_BAD_ACCESS next time you use this variable.
// So android gives you JNI ERROR (app bug) error to prevent you from getting undebugable EXC_BAD_ACCESS.
// Now there are two ways to avoid this error. You can set targetSdkVersion in your manifest to version 11 or less.
// This will enable JNI bug compatibility mode and prevent any problems altogether.
// This is the reason why your old examples are working.
// You can avoid using static variables pointing to java objects or
// make jobject references global before storing them by calling env->NewGlobalRef(ref).
// Perhaps on of the biggest examples here is keeping jclass objects.
// Normally, you'll initialize static jclass variable during JNI_OnLoad,
// since class objects remain in the memory as long as the application is running.
//
//--------------------------------------------------------------------------------
// This code will lead to a crash:
//
// static jclass myClass;
//
// JNIEXPORT jint JNICALL JNI_OnLoad (JavaVM * vm, void * reserved) {
//     myClass = env->FindClass("com/example/company/MyClass");
//     return JNI_VERSION_1_6;
// }
//
//--------------------------------------------------------------------------------
// While this code will run fine:
//
// static jclass myClass;
//
// JNIEXPORT jint JNICALL JNI_OnLoad (JavaVM * vm, void * reserved) {
//     jclass tmp = env->FindClass("com/example/company/MyClass");
//     myClass = (jclass)env->NewGlobalRef(tmp);
//     return JNI_VERSION_1_6;
// }

//------------------------------------------------------------------------------
JNIEXPORT void JNICALL MP_JniInit( JNIEnv *env, jclass obj, jstring className, jstring callbackName )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );

	const char *pClassName = env->GetStringUTFChars( className, 0 );
	strcpy( gClassName, pClassName );
	env->ReleaseStringUTFChars( className, pClassName );

	const char *pCallbackName = env->GetStringUTFChars( callbackName, 0 );
	strcpy( gCallbackName, pCallbackName );
	env->ReleaseStringUTFChars( callbackName, pCallbackName );

	env->GetJavaVM( &gJavaVM );
	jclass findClass = env->FindClass( gClassName );
	gClass = (jclass)env->NewGlobalRef( findClass );
	
	if( !gClass  ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): FindClass() failed.\n", __FUNCTION__);
		return;
	}

	if( !(gMethodID = env->GetStaticMethodID( gClass, gCallbackName, "(II)V" )) ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): GetStaticMethodID() failed.\n", __FUNCTION__);
	}

	pthread_mutex_init( &hLock, NULL );

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}

//------------------------------------------------------------------------------
JNIEXPORT void JNICALL MP_JniDeinit( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	
	pthread_mutex_destroy( &hLock );
	
	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_Open( JNIEnv *env, jclass obj, jstring uri )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );
	
	if( NULL != hMoviePlayer ) {
		NxDbgMsg( NX_DBG_VBS, "%s(): Error! Handle is already initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = MP_ERR_NONE;

	mpResult = NX_MPOpen( &hMoviePlayer, &EventCallback, NULL );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_VBS, "%s(): Error! NX_MPOpen() Failed! (ret = %d )", __FUNCTION__, mpResult);
		return mpResult;
	}

	NxDbgMsg( NX_DBG_DEBUG, "%s(): hMoviePlayer( %p )\n", __FUNCTION__, hMoviePlayer );

	for( int i = 0; i < MAX_DISPLAY_CHANNEL; i++ ){
		gpNativeWindow[i] = NULL;
		gpDspConfig[i] = NULL;
	}

	const char *pBuf = env->GetStringUTFChars( uri, 0 );
	strcpy( gUriBuf, pBuf );
	env->ReleaseStringUTFChars( uri, pBuf );

	NxDbgMsg( NX_DBG_INFO, "%s(): UriName:: %s", __FUNCTION__, gUriBuf );

	mpResult = NX_MPSetUri( hMoviePlayer, gUriBuf );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPSetUri() Failed! (ret = %d, uri = %s)", __FUNCTION__, mpResult, gUriBuf );
	}
	else {
		mpResult = NX_MPGetMediaInfo( hMoviePlayer, &gMediaInfo );
		if( MP_ERR_NONE != mpResult ) {
			NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPGetMediaInfo() Failed! (ret = %d)", __FUNCTION__, mpResult );
		}
	}

	// PrintMediaInfo();

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT void JNICALL MP_Close( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	MP_RESULT mpResult = NX_MPClearTrack( hMoviePlayer );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPClearTrack() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	for( int i = 0; i < MAX_DISPLAY_CHANNEL; i++ )
	{
		if( gpNativeWindow[i] ) {
			ANativeWindow_release( gpNativeWindow[i] );
		}
		gpNativeWindow[i] = NULL;

		if( gpDspConfig[i] ) {
			free( gpDspConfig[i] );
		}
		gpDspConfig[i] = NULL;
	}
	
	if( hMoviePlayer ) {
		NX_MPClose( hMoviePlayer );
		hMoviePlayer = NULL;	
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}

//------------------------------------------------------------------------------
static char *GetCodecId( int codecId )
{
	for( int i = 0; i < sizeof(gCodecType) / sizeof(gCodecType[0]); i++ )
	{
		if( gCodecType[i].CodecId == codecId )
			return gCodecType[i].CodecString;
	}

	return NULL;
}

//------------------------------------------------------------------------------
JNIEXPORT jstring JNICALL MP_GetMediaInfo( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	// FIXME!!!!!!!!!!!
	char destBuf[8192] = { 0x00, };

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return env->NewStringUTF(destBuf);
	}

	int pos = 0;
	pos += sprintf( destBuf + pos, "FileName :\n  %s\n\n", gUriBuf );

	for( int32_t i = 0; i < gMediaInfo.iProgramNum; i++ )
	{
		if( 1 < gMediaInfo.iProgramNum ) 
			pos += sprintf( destBuf + pos, "*** Program #%d ***\n", i);
		
		if( 0 < gMediaInfo.ProgramInfo[i].iVideoNum )
		{
			int num = 0;
			pos += sprintf( destBuf + pos, "[ Video Information ]\n" );
			
			for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
			{
				MP_TRACK_INFO *pTrackInfo = &gMediaInfo.ProgramInfo[i].TrackInfo[j];

				if( MP_TRACK_VIDEO == pTrackInfo->iTrackType ) {
					pos += sprintf( destBuf + pos, " Video Track #%d\n", num++ );
					pos += sprintf( destBuf + pos, "  -. Track Index\t\t\t: %d\n", pTrackInfo->iTrackIndex );
					char *pCodec = GetCodecId((int)pTrackInfo->iCodecId);
					pos += sprintf( destBuf + pos, "  -. Codec Type\t\t\t: %s\n", (pCodec == NULL) ? "Unknown" : pCodec );
					pos += sprintf( destBuf + pos, "  -. Resolution\t\t\t: %d x %d\n", pTrackInfo->iWidth, pTrackInfo->iHeight );
					pos += sprintf( destBuf + pos, "  -. Duration\t\t\t\t: %lld ms\n\n", pTrackInfo->iDuration );
				}	
			}
		}

		if( 0 < gMediaInfo.ProgramInfo[i].iAudioNum )
		{
			int num = 0;
			pos += sprintf( destBuf + pos, "[ Audio Information ]\n" );

			for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
			{
				MP_TRACK_INFO *pTrackInfo = &gMediaInfo.ProgramInfo[i].TrackInfo[j];
				if( MP_TRACK_AUDIO == pTrackInfo->iTrackType ) {
					pos += sprintf( destBuf + pos, " Audio Track #%d\n", num++ );
					pos += sprintf( destBuf + pos, "  -. Track Index\t\t\t: %d\n", pTrackInfo->iTrackIndex );
					char *pCodec = GetCodecId((int)pTrackInfo->iCodecId);
					pos += sprintf( destBuf + pos, "  -. Codec Type\t\t\t: %s\n", (pCodec == NULL) ? "Unknown" : pCodec );
					pos += sprintf( destBuf + pos, "  -. Channels\t\t\t\t: %d\n", pTrackInfo->iChannels );
					pos += sprintf( destBuf + pos, "  -. SampleRate\t\t: %d Hz\n", pTrackInfo->iSampleRate );
					pos += sprintf( destBuf + pos, "  -. Bitrate\t\t\t\t\t: %d bps\n", pTrackInfo->iBitrate );
					pos += sprintf( destBuf + pos, "  -. Duration\t\t\t\t: %lld ms\n\n", pTrackInfo->iDuration );
				}					
			}
		}
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return env->NewStringUTF(destBuf);
}

//------------------------------------------------------------------------------
static int GetTrackIndex( int trackType, int track )
{
	int index = -1, trackOrder = 0;

	for( int i = 0; i < gMediaInfo.iProgramNum; i++ )
	{
		for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
		{
			if( trackType == gMediaInfo.ProgramInfo[i].TrackInfo[j].iTrackType )
			{
				if( track == trackOrder )
				{
					index = gMediaInfo.ProgramInfo[i].TrackInfo[j].iTrackIndex;
					break;
				}
				trackOrder++;
			}
		}

		if( index != -1 )
			break;
	}

	// NxDbgMsg( NX_DBG_INFO, "[%s] Require Track( %d ), Stream Index( %d )", (trackType == MP_TRACK_AUDIO) ? "AUDIO" : "VIDEO", track, index );
	return index;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_GetVideoWidth( JNIEnv *env, jclass obj, jint track )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	int ret = -1, trackOrder = 0;

	for( int i = 0; i < gMediaInfo.iProgramNum; i++ )
	{
		for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
		{
			if( MP_TRACK_VIDEO == gMediaInfo.ProgramInfo[i].TrackInfo[j].iTrackType )
			{
				if( track == trackOrder )
				{
					ret = gMediaInfo.ProgramInfo[i].TrackInfo[j].iWidth;
					break;
				}
				trackOrder++;
			}
		}
	}

	return ret;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_GetVideoHeight( JNIEnv *env, jclass obj, jint track )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	int ret = -1, trackOrder = 0;

	for( int i = 0; i < gMediaInfo.iProgramNum; i++ )
	{
		for( int j = 0; j < gMediaInfo.ProgramInfo[i].iVideoNum + gMediaInfo.ProgramInfo[i].iAudioNum; j++ )
		{
			if( MP_TRACK_VIDEO == gMediaInfo.ProgramInfo[i].TrackInfo[j].iTrackType )
			{
				if( track == trackOrder )
				{
					ret = gMediaInfo.ProgramInfo[i].TrackInfo[j].iHeight;
					break;
				}
				trackOrder++;
			}
		}
	}

	return ret;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_GetVideoTrackNum( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return gMediaInfo.iVideoTrackNum;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_GetAudioTrackNum( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return gMediaInfo.iAudioTrackNum;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_AddVideoTrack( JNIEnv *env, jclass obj, jint track, jobject jSurface )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	int index = GetTrackIndex( MP_TRACK_VIDEO, track );
	if( 0 > index ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Get Video Index. ( track = %d )", __FUNCTION__, track );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( NULL != jSurface ) {
		if( NULL != gpNativeWindow[track] ) {
			NxDbgMsg( NX_DBG_ERR, "%s(): Error! ANativeBuffer Slot is not empty.", __FUNCTION__ );
			NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
			return -1;
		}

		gpNativeWindow[track] = ANativeWindow_fromSurface( env, jSurface );
	}
	else {
		if( NULL == gpDspConfig[track] ) {
			NxDbgMsg( NX_DBG_ERR, "%s(): Error! Invalid VideoConfig.", __FUNCTION__ );
			NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
			return -1;
		}
	}

	MP_RESULT mpResult = NX_MPAddTrack( hMoviePlayer, index, gpNativeWindow[track], gpDspConfig[track] );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPAddTrack() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_AddVideoConfig( JNIEnv *env, jclass obj, jint track, jint port, jint module, jint srcX, jint srcY, jint srcWidth, jint srcHeight, jint dstX, jint dstY, jint dstWidth, jint dstHeight )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( NULL != gpDspConfig[track] )
	{
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! VideoConfig Slot is not empty.", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	gpDspConfig[track] = (MP_DSP_CONFIG*)malloc( sizeof(MP_DSP_CONFIG) );
	gpDspConfig[track]->iPort			= port;
	gpDspConfig[track]->iModule			= module;

	gpDspConfig[track]->srcRect.iX		= srcX;
	gpDspConfig[track]->srcRect.iY		= srcY;
	gpDspConfig[track]->srcRect.iWidth	= srcWidth;
	gpDspConfig[track]->srcRect.iHeight	= srcHeight;

	gpDspConfig[track]->dstRect.iX		= dstX;
	gpDspConfig[track]->dstRect.iY		= dstY;
	gpDspConfig[track]->dstRect.iWidth	= dstWidth;
	gpDspConfig[track]->dstRect.iHeight	= dstHeight;

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return 0;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_SetVideoCrop( JNIEnv *env, jclass obj, jint track, jint x, jint y, jint width, jint height )\
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_DSP_RECT rect;
	rect.iX		= x;
	rect.iY		= y;
	rect.iWidth	= width;
	rect.iHeight= height;

	MP_RESULT mpResult = NX_MPSetDspCrop( hMoviePlayer, track, &rect );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPSetDspPosition() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return 0;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_SetVideoPosition( JNIEnv *env, jclass obj, jint track, jint x, jint y, jint width, jint height )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_DSP_RECT rect;
	rect.iX		= x;
	rect.iY		= y;
	rect.iWidth	= width;
	rect.iHeight= height;

	MP_RESULT mpResult = NX_MPSetDspPosition( hMoviePlayer, track, &rect );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPSetDspPosition() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return 0;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_SetVideoLayerPriority( JNIEnv *env, jclass obj, jint track, jint module, jint priority )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}
	
	MP_RESULT mpResult = NX_MPSetVideoLayerPriority( hMoviePlayer, track, module, priority );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPSetVideoLayerPriority() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return 0;	
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_AddSubDisplay( JNIEnv *env, jclass obj, jint track, jint port, jint module, jint srcX, jint srcY, jint srcWidth, jint srcHeight, jint dstX, jint dstY, jint dstWidth, jint dstHeight )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_DSP_CONFIG config;
	config.iPort			= port;
	config.iModule			= module;
	
	config.srcRect.iX		= srcX;
	config.srcRect.iY		= srcY;
	config.srcRect.iWidth	= srcWidth;
	config.srcRect.iHeight	= srcHeight;

	config.dstRect.iX		= dstX;
	config.dstRect.iY		= dstY;
	config.dstRect.iWidth	= dstWidth;
	config.dstRect.iHeight	= dstHeight;

	MP_RESULT mpResult = NX_MPAddSubDisplay( hMoviePlayer, track, &config );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPAddSubDisplay() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_ClearSubDisplay( JNIEnv *env, jclass obj, jint track )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.iVideoTrackNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / videoTrack = %d)\n", __FUNCTION__, track, gMediaInfo.iVideoTrackNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPClearSubDisplay( hMoviePlayer, track );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPClearSubDisplay() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_AddAudioTrack( JNIEnv *env, jclass obj, jint track )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	if( track >= gMediaInfo.ProgramInfo[0].iAudioNum ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Track Number. (track = %d / audioTrack = %d)\n", __FUNCTION__, track, gMediaInfo.ProgramInfo[0].iAudioNum );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	int index = GetTrackIndex( MP_TRACK_AUDIO, track );
	if( 0 > index ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Get Audio Index. ( track = %d )", __FUNCTION__, track );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPAddTrack( hMoviePlayer, index, NULL, NULL );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPAddTrack() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;	
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_ClearTrack( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPClearTrack( hMoviePlayer );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPClearTrack() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	for( int i = 0; i < MAX_DISPLAY_CHANNEL; i++ )
	{
		if( gpNativeWindow[i] ) {
			ANativeWindow_release( gpNativeWindow[i] );
		}
		gpNativeWindow[i] = NULL;
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return 0;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_Play(JNIEnv *env, jclass obj)
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPPlay( hMoviePlayer );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPPlay() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	bPlay = true;
	bPause = false;

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_Stop(JNIEnv *env, jclass obj)
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPStop( hMoviePlayer );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPStop() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	bPlay = false;
	bPause = false;

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_Pause(JNIEnv *env, jclass obj)
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s: Error! Handle is not initialized!", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPPause( hMoviePlayer );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPPause() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	bPlay = false;
	bPause = true;

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_Seek( JNIEnv *env, jclass obj, jint seekTime )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Handle is not initialized!", __FUNCTION__);
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	MP_RESULT mpResult = NX_MPSeek( hMoviePlayer, seekTime );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPSeek() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return mpResult;
}

//------------------------------------------------------------------------------
JNIEXPORT jlong JNICALL MP_GetDuration( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Handle is not initialized!", __FUNCTION__);
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	long long duration;
	MP_RESULT mpResult = NX_MPGetDuration( hMoviePlayer, &duration );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPGetDuration() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return duration;
}

//------------------------------------------------------------------------------
JNIEXPORT jlong JNICALL MP_GetPosition( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Handle is not initialized!", __FUNCTION__);
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	long long position;
	MP_RESULT mpResult = NX_MPGetPosition( hMoviePlayer, &position );
	if( MP_ERR_NONE != mpResult ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! NX_MPGetPosition() Failed! (ret = %d)", __FUNCTION__, mpResult);
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return position;
}

//------------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL MP_IsPlay( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( NULL == hMoviePlayer ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Handle is not initialized!", __FUNCTION__);
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
		return -1;
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return bPlay;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_MakeThumbnail( JNIEnv *env, jclass obj, jstring inUri, jstring outUri, jint outWidth, jint outHeight )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	const char *pInFile		= env->GetStringUTFChars( inUri, 0 );
	const char *pOutFile	= env->GetStringUTFChars( outUri, 0 );

	int ret = NX_MPMakeThumbnail( pInFile, pOutFile, 320, 320, 10 );

	env->ReleaseStringUTFChars( inUri, pInFile );
	env->ReleaseStringUTFChars( outUri, pOutFile );

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return ret;
}

//------------------------------------------------------------------------------
JNIEXPORT jint JNICALL MP_GetVersion( JNIEnv *env, jclass obj )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return NX_MPGetVersion();
}

//------------------------------------------------------------------------------
JNIEXPORT void JNICALL MP_ChgDebugLevel( JNIEnv *env, jclass obj, jint level )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );
	CNX_AutoLock lock( &hLock );

	if( level < 0 || level > 5 ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Error! Unknown Debug level!\n", __FUNCTION__ );
		NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );	
	}
	else {
		NX_MPChgDebugLevel( level );
	}

	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}

//------------------------------------------------------------------------------
//
//	Implementation JNI_OnLoad()
//
static JNINativeMethod sMethods[] = {
	//	Native Function Name,			Sigunature, 						C++ Function Name
	{ "MP_JniInit",			"(Ljava/lang/String;Ljava/lang/String;)V", 		(void*)MP_JniInit 				},
	{ "MP_JniDeinit",					"()V",								(void*)MP_JniDeinit				},
	{ "MP_Open",						"(Ljava/lang/String;)I",			(void*)MP_Open					},
	{ "MP_Close",						"()V",								(void*)MP_Close					},
	{ "MP_GetMediaInfo",				"()Ljava/lang/String;", 			(void*)MP_GetMediaInfo			},
	{ "MP_GetVideoWidth",				"(I)I",								(void*)MP_GetVideoWidth			},
	{ "MP_GetVideoHeight",				"(I)I",								(void*)MP_GetVideoHeight		},
	{ "MP_GetVideoTrackNum",			"()I",								(void*)MP_GetVideoTrackNum		},
	{ "MP_GetAudioTrackNum",			"()I",								(void*)MP_GetAudioTrackNum		},
	{ "MP_AddVideoTrack",				"(ILandroid/view/Surface;)I", 		(void*)MP_AddVideoTrack			},
	{ "MP_AddVideoConfig",				"(IIIIIIIIIII)I",					(void*)MP_AddVideoConfig		},
	{ "MP_SetVideoCrop",				"(IIIII)I",							(void*)MP_SetVideoCrop			},
	{ "MP_SetVideoPosition",			"(IIIII)I",							(void*)MP_SetVideoPosition		},
	{ "MP_SetVideoLayerPriority",		"(III)I",							(void*)MP_SetVideoLayerPriority	},
	{ "MP_AddSubDisplay",				"(IIIIIIIIIII)I",					(void*)MP_AddSubDisplay			},
	{ "MP_ClearSubDisplay",				"(I)I",								(void*)MP_ClearSubDisplay		},
	{ "MP_AddAudioTrack",				"(I)I",								(void*)MP_AddAudioTrack			},
	{ "MP_ClearTrack",					"()I",								(void*)MP_ClearTrack			},
	{ "MP_Play",						"()I",								(void*)MP_Play					},
	{ "MP_Stop",						"()I",								(void*)MP_Stop					},
	{ "MP_Pause",						"()I",								(void*)MP_Pause					},
	{ "MP_Seek",						"(I)I",								(void*)MP_Seek					},
	{ "MP_GetDuration",					"()J",								(void*)MP_GetDuration			},
	{ "MP_GetPosition",					"()J",								(void*)MP_GetPosition			},
	{ "MP_IsPlay",						"()Z",								(void*)MP_IsPlay				},
	{ "MP_MakeThumbnail",	"(Ljava/lang/String;Ljava/lang/String;II)I", 	(void*)MP_MakeThumbnail			},
	{ "MP_GetVersion",					"()I",								(void*)MP_GetVersion			},
	{ "MP_ChgDebugLevel",				"(I)V",								(void*)MP_ChgDebugLevel			},
};

//------------------------------------------------------------------------------
static int RegisterNativeMethods( JNIEnv *env, const char *className, JNINativeMethod *gMethods, int numMethods )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );

	jclass clazz;
	int result = JNI_FALSE;

	clazz = env->FindClass( className );
	if( clazz == NULL ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): Native registration unable to find class '%s'", __FUNCTION__, className );
		goto FAIL;
	}

	if( env->RegisterNatives( clazz, gMethods, numMethods) < 0 ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): RegisterNatives failed for '%s'", __FUNCTION__, className);
		goto FAIL;
	}

	result = JNI_TRUE;

FAIL:
	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return result;
}

//------------------------------------------------------------------------------
jint JNI_OnLoad( JavaVM *vm, void *reserved )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );

	jint result = -1;
	JNIEnv *env = NULL;

	if( vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): GetEnv failed!\n", __FUNCTION__ );
		goto FAIL;
	}

	if( RegisterNativeMethods(env, "com/example/nxplayerbasedfilter/MoviePlayer", sMethods, sizeof(sMethods) / sizeof(sMethods[0]) ) != JNI_TRUE ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): RegisterNativeMethods failed!", __FUNCTION__ );
		goto FAIL;
	}

	result = JNI_VERSION_1_4;

FAIL:
	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
	return result;
}

//------------------------------------------------------------------------------
void JNI_OnUnload( JavaVM *vm, void *reserved )
{
	NxDbgMsg( NX_DBG_VBS, "%s()++", __FUNCTION__ );

	JNIEnv *env = NULL;

	if( vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK ) {
		NxDbgMsg( NX_DBG_ERR, "%s(): GetEnv failed!", __FUNCTION__ );
		goto FAIL;
	}

FAIL:
	NxDbgMsg( NX_DBG_VBS, "%s()--", __FUNCTION__ );
}
