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

package com.example.nxplayerbasedfilter;

import android.view.Surface;
import android.util.Log;

enum DISPLAY_PORT		{ LCD, HDMI }
enum DISPLAY_MODULE		{ MLC0, MLC1 }

class NX_CDisplayRect {
	public int x		= 0;
	public int y		= 0;
	public int width	= 0;
	public int height	= 0;

	public NX_CDisplayRect() {
	}
}

class NX_CDisplayConfig {
	public int				port;
	public int				module;
	public NX_CDisplayRect	srcRect;
	public NX_CDisplayRect	dstRect;

	public NX_CDisplayConfig() {
	}
}

public class MoviePlayer {
	private static final String DBG_TAG = "MoviePlayer";

	private static final int NX_DBG_VBS		= 0;
	private static final int NX_DBG_DEBUG	= 1;
	private static final int NX_DBG_INFO	= 2;
	private static final int NX_DBG_WARN	= 3;
	private static final int NX_DBG_ERR		= 4;
	private static final int NX_DBG_DISABLE = 5;
		
	private static MoviePlayer mInstance;
	private static boolean mRun = true;

	private static int mRunCount = 0;

	private MoviePlayer() {
		MP_JniInit( "com/example/nxplayerbasedfilter/PlayerActivity", "EventHandler" );

		int version = MP_GetVersion();
		int major	= ( version & 0xFF000000 ) >> 24;
		int minor	= ( version & 0x00FF0000 ) >> 16;
		int revision= ( version & 0x0000FF00 ) >> 8;
		Log.i( DBG_TAG, "Filter Version : " + String.valueOf(major) + "." + String.valueOf(minor) + "." + String.valueOf(revision) );

		MP_ChgDebugLevel( NX_DBG_INFO );
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		MP_JniDeinit();
	}
	
	public static synchronized MoviePlayer GetInstance() {
		if( mInstance == null ) {
			mInstance = new MoviePlayer();
		}
		return mInstance;
	}
	
	public synchronized int Open( String uri )
	{
		String strTemp = "[ " + String.valueOf(++mRunCount) + " ] uri : " + uri;
		Log.i( DBG_TAG, strTemp );

		return MP_Open( uri );
	}
	
	public synchronized void Close()
	{
		MP_Close();
	}

	public synchronized String GetMediaInfo()
	{
		return MP_GetMediaInfo();
	}

	public synchronized int GetVideoWidth( int track )
	{
		return MP_GetVideoWidth( track );
	}

	public synchronized int GetVideoHeight( int track )
	{
		return MP_GetVideoHeight( track );
	}

	public synchronized int GetVideoTrackNum()
	{
		return MP_GetVideoTrackNum();
	}
	
	public synchronized int GetAudioTrackNum()
	{
		return MP_GetAudioTrackNum();
	}

	public synchronized int AddVideoTrack( int track, Surface sf )
	{
		return MP_AddVideoTrack( track, sf );
	}

	public synchronized int AddAudioTrack( int track )
	{
		return MP_AddAudioTrack( track );
	}

	public synchronized int ClearTrack()
	{
		return MP_ClearTrack();
	}

	public synchronized int AddVideoConfig( int track, DISPLAY_PORT port, DISPLAY_MODULE module, NX_CDisplayRect srcRect, NX_CDisplayRect dstRect )
	{
		int iPort	= (port == DISPLAY_PORT.LCD)		? 0 : 1;
		int iModule = (module == DISPLAY_MODULE.MLC0)	? 0 : 1;

		return MP_AddVideoConfig( track, iPort, iModule, srcRect.x, srcRect.y, srcRect.width, srcRect.height, dstRect.x, dstRect.y, dstRect.width, dstRect.height );
	}

	public synchronized int AddSubDisplay( int track, DISPLAY_PORT port, DISPLAY_MODULE module, NX_CDisplayRect srcRect, NX_CDisplayRect dstRect )
	{
		int iPort	= (port == DISPLAY_PORT.LCD)		? 0 : 1;
		int iModule = (module == DISPLAY_MODULE.MLC0)	? 0 : 1;

		return MP_AddSubDisplay( track, iPort, iModule, srcRect.x, srcRect.y, srcRect.width, srcRect.height, dstRect.x, dstRect.y, dstRect.width, dstRect.height );
	}

	public synchronized int ClearSubDisplay( int track )
	{
		return MP_ClearSubDisplay( track );
	}
	
	public synchronized int SetVideoPosition( int track, NX_CDisplayRect rect )
	{
		return MP_SetVideoPosition( track, rect.x, rect.y, rect.width, rect.height );
	}

	public synchronized int SetVideoCrop( int track, NX_CDisplayRect rect )
	{
		return MP_SetVideoCrop( track, rect.x, rect.y, rect.width, rect.height );
	}

	public synchronized int SetVideoLayerPriority( int track, DISPLAY_MODULE module, int priority )
	{
		int iModule = (module == DISPLAY_MODULE.MLC0)	? 0 : 1;

		return MP_SetVideoLayerPriority( track, iModule, priority ); 
	}

	public synchronized int Play()
	{
		return MP_Play();
	}

	public synchronized int Stop()
	{
		return MP_Stop();
	}

	public synchronized int Pause()
	{
		return MP_Pause();
	}
	
	public synchronized int Seek( int seekTime )
	{
		return MP_Seek( seekTime );
	}
	
	public synchronized long GetDuration()
	{
		return MP_GetDuration();
	}
	
	public synchronized long GetPosition()
	{
		return MP_GetPosition();
	}
	
	public synchronized boolean IsPlay()
	{
		return MP_IsPlay();
	}
	
	public synchronized int MakeThumbnail( String inUri, String outUri, int outWidth, int outHeight )
	{
		return MP_MakeThumbnail( inUri, outUri, outWidth, outHeight );
	}

	public synchronized int GetVersion()
	{
		return MP_GetVersion();
	}

	public synchronized void ChgDebugLevel( int level )
	{
		MP_ChgDebugLevel( level );
	}

	static {
		System.loadLibrary("nxmovieplayer");
	}
	
	public native void		MP_JniInit( String className, String callbackName );
	public native void		MP_JniDeinit();
	public native int		MP_Open( String uri );
	public native void		MP_Close();
	public native String	MP_GetMediaInfo();
	public native int		MP_GetVideoWidth( int track );
	public native int		MP_GetVideoHeight( int track );
	public native int		MP_GetVideoTrackNum();
	public native int		MP_GetAudioTrackNum();
	public native int		MP_AddVideoTrack( int track, Surface sf );
	public native int		MP_AddAudioTrack( int track );
	public native int		MP_ClearTrack();
	public native int		MP_AddSubDisplay( int track, int port, int module, int srcX, int srcY, int srcWidth, int srcHeight, int dstX, int dstY, int dstWidth, int dstHeight );
	public native int		MP_ClearSubDisplay( int track );
	public native int		MP_AddVideoConfig( int track, int port, int module, int srcX, int srcY, int srcWidth, int srcHeight, int dstX, int dstY, int dstWidth, int dstHeight );
	public native int		MP_SetVideoCrop( int track, int x, int y, int width, int height );
	public native int		MP_SetVideoPosition( int track, int x, int y, int width, int height );
	public native int		MP_SetVideoLayerPriority( int track, int module, int priority );
	public native int		MP_Play();
	public native int		MP_Stop();
	public native int		MP_Pause();
	public native int		MP_Seek( int seekTime );
	public native long		MP_GetDuration();
	public native long		MP_GetPosition();
	public native boolean	MP_IsPlay();
	public native int		MP_MakeThumbnail( String inUri, String outUri, int outWidth, int outHeight );
	public native int		MP_GetVersion();
	public native void		MP_ChgDebugLevel( int level );
}
