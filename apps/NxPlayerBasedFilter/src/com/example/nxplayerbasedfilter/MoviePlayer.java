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

public class MoviePlayer {
	private static final String DBG_TAG = "NxPlayerBasedFilter.MoviePlayer";
	
	private static MoviePlayer mInstance;

	private MoviePlayer() {
		Mp_JniInit();
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		Mp_JniDeinit();
	}
	
	public static synchronized MoviePlayer GetInstance() {
		if( mInstance == null ) {
			mInstance = new MoviePlayer();
		}
		return mInstance;
	}
	
	public synchronized int SetFileName( String uri )
	{
		Log.i(DBG_TAG, "SetFileName : " + uri);
		return Mp_SetFileName( uri );
	}
	
	public synchronized int GetVideoTrackNum()
	{
		return Mp_GetVideoTrackNum();
	}
	
	public synchronized String GetMediaInfo()
	{
		return Mp_GetMediaInfo();
	}
	
	public synchronized int Open( Surface sf1, Surface sf2, int vidRequest, boolean pipOn )
	{
		return Mp_Open( sf1, sf2, vidRequest, pipOn );
	}
	
	public synchronized int Close()
	{
		return Mp_Close();
	}
	
	public synchronized int Play()
	{
		return Mp_Play();
	}
	
	public synchronized int Pause()
	{
		return Mp_Pause();
	}
	
	public synchronized int Stop()
	{
		return Mp_Stop();
	}
	
	public synchronized int Seek( int seekTime )
	{
		return Mp_Seek( seekTime );
	}
	
	public synchronized int GetDuration()
	{
		return Mp_GetCurDuration();
	}
	
	public synchronized int GetCurPosition()
	{
		return Mp_GetCurPosition();
	}
	
	public synchronized int IsPlay()
	{
		return Mp_IsPlay();
	}
	
	static {
		System.loadLibrary("nxmovieplayer");
	}
	
	public native void	Mp_JniInit();
	public native void	Mp_JniDeinit();
	public native int	Mp_SetFileName( String uri );
	public native int 	Mp_GetVideoTrackNum();
	public native String Mp_GetMediaInfo();
	public native int 	Mp_GetVideoWidth( int vidRequest );
	public native int	Mp_GetVideoHeight( int vidRequest );
	public native int 	Mp_Open( Surface sf1, Surface sf2, int vidRequest, boolean pipOn );
	public native int 	Mp_Close();
	public native int 	Mp_Play();
	public native int 	Mp_Pause();
	public native int 	Mp_Stop();
	public native int 	Mp_IsPlay();
	public native int 	Mp_Seek( int seekTime );
	public native int 	Mp_GetCurDuration();
	public native int 	Mp_GetCurPosition();
}

