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

import com.example.nxplayerbasedfilter.MainActivity;

import android.app.Presentation;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Display;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.graphics.Color;

import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

import android.graphics.Rect;
import android.graphics.RectF;

// import android.app.AlertDialog;
// import android.content.DialogInterface;

import android.widget.TextView;

public class PlayerPresentation extends Presentation implements SurfaceHolder.Callback {
	private static final String DBG_TAG 			= "PlayerPresentation";

	private static final boolean RENDER_VIDEOLAYER	= false;
	
	private static final boolean DEBUG_RENDERER		= true;
	private static final boolean DEBUG_MEDIAINFO	= true;

	private static final boolean ASING_RUN			= true;

	public static Context	mContext;

	Bitmap			mBmpImage;
	int				mBmpWidth;
	int				mBmpHeight;
	
	RelativeLayout	mLayOut;
	
	Surface			mSurface1;
	SurfaceView		mSurfaceView1;
	SurfaceHolder	mSurfaceHolder1;

	Surface			mSurface2;
	SurfaceView		mSurfaceView2;
	SurfaceHolder	mSurfaceHolder2;

	String			mVideoName;
	String			mMediaInfo;
	
	int				mVideoTrackNum;
	int				mAudioTrackNum;

	int				mDisplayMode = 0; 
	
	int				mDisplayModeChange;
	int				mScreenWidth = 1920, mScreenHeight = 1080;
	int				mVidWidth, mVidHeight;
	int				mScreenRotate;
	
	MoviePlayer		mMoviePlayer;

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Activity Override Function
	//
	public PlayerPresentation(Context context, Display display) {
		super(context, display);
		
		mContext	= context;
		mMoviePlayer= ((PlayerActivity)PlayerActivity.mContext).mMoviePlayer;
	}

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		FUNCIN();
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView( R.layout.presentation_player );

		// TODO Auto-generated method stub
		mLayOut 		= (RelativeLayout)findViewById( R.id.playerLayOut );
		
		mSurfaceView1 	= (SurfaceView)findViewById( R.id.surfaceView1 );
		mSurfaceView2	= (SurfaceView)findViewById( R.id.surfaceView2 );
		
		mVideoName 		= ((MainActivity)MainActivity.mContext).GetFileName();

		// Bitmap image load
		Resources res = mContext.getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();
		
		mBmpImage = BitmapFactory.decodeResource(res, R.drawable.audio_only, options);
		mBmpWidth = options.outWidth;
		mBmpHeight = options.outHeight;

		mLayOut.setBackgroundColor( Color.rgb( 0x00,0x00,0x00 ) );
		SetDisplayLayout();

		FUNCOUT();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Surface Callback
	//
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		FUNCIN();
		int ret = 0;

		if( mSurface1 == null && mSurface2 == null ) {
			if( 0 > mMoviePlayer.Open( mVideoName ) )
			{
				if( ASING_RUN )
				{
					PlayerNextFile();
				}
				else
				{

				}

				FUNCOUT();
				return ;
			}
			mVideoTrackNum = mMoviePlayer.GetVideoTrackNum();
			mAudioTrackNum = mMoviePlayer.GetAudioTrackNum();

			if( 0 < mAudioTrackNum )
			{
				if( 0 > (ret = mMoviePlayer.AddAudioTrack( 0 )) )
				{
					if( ASING_RUN )
					{					
						if( 0 == mVideoTrackNum )
							PlayerNextFile();
					}
					else
					{

					}
					FUNCOUT();
					return ;
				}

				if( 0 == mVideoTrackNum ) {
					Rect 	srcImage = new Rect();
					RectF 	dstImage = new RectF();
					
					srcImage.left	= 0;
					srcImage.top	= 0;
					srcImage.right	= mBmpWidth;
					srcImage.bottom	= mBmpHeight;
					
					dstImage.left 	= mScreenWidth / 2 - mBmpWidth;
					dstImage.top 	= mScreenHeight / 2 - mBmpHeight;
					dstImage.right 	= mScreenWidth / 2 + mBmpWidth;
					dstImage.bottom = mScreenHeight / 2 + mBmpHeight;;
					
					Canvas canvas = mSurfaceHolder1.lockCanvas();
					canvas.drawRGB(0x00,  0x00,  0x00);
					canvas.drawBitmap(mBmpImage, srcImage, dstImage, null);
					mSurfaceHolder1.unlockCanvasAndPost(canvas);
					
					mMoviePlayer.Play();
					
					FUNCOUT();
					return;
				}
			}
		}
		
		// Video Case
		if( mSurfaceHolder1 == holder && mSurface1 == null ) {
			mSurface1 = mSurfaceHolder1.getSurface();
			if( mDisplayMode == 0 || mDisplayMode == 2 ) {
				if( RENDER_VIDEOLAYER )
				{
					mVidWidth = GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
					mVidHeight = GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );

					NX_CDisplayRect srcRect = new NX_CDisplayRect();
					NX_CDisplayRect dstRect = new NX_CDisplayRect();

					dstRect.x		= mScreenWidth / 2 - mVidWidth / 2;
					dstRect.y		= mScreenHeight / 2 - mVidHeight / 2;
					dstRect.width	= mVidWidth;
					dstRect.height	= mVidHeight;

					mMoviePlayer.AddVideoConfig( 0, DISPLAY_PORT.HDMI, DISPLAY_MODULE.MLC1, srcRect, dstRect );

					ret = mMoviePlayer.AddVideoTrack( 0, null );
					mMoviePlayer.SetVideoLayerPriority( 1, DISPLAY_MODULE.MLC1, 1 );				
				}
				else
				{
					ret = mMoviePlayer.AddVideoTrack( 0, mSurface1 );
				}
			}
		}
		if( mSurfaceHolder2 == holder && mSurface2 == null ) {
			mSurface2 = mSurfaceHolder2.getSurface();
			if( mDisplayMode == 1 || mDisplayMode == 2 ) {
				if( RENDER_VIDEOLAYER )
				{
					mVidWidth = GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
					mVidHeight = GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );

					NX_CDisplayRect srcRect = new NX_CDisplayRect();
					NX_CDisplayRect dstRect = new NX_CDisplayRect();
					
					dstRect.x		= mScreenWidth / 2 - mVidWidth / 2;
					dstRect.y		= mScreenHeight / 2 - mVidHeight / 2;
					dstRect.width	= mVidWidth;
					dstRect.height	= mVidHeight;

					mMoviePlayer.AddVideoConfig( 1, DISPLAY_PORT.HDMI, DISPLAY_MODULE.MLC1, srcRect, dstRect );

					ret = mMoviePlayer.AddVideoTrack( 1, null );	
					mMoviePlayer.SetVideoLayerPriority( 1, DISPLAY_MODULE.MLC1, 1 );
				}
				else
				{
					ret = mMoviePlayer.AddVideoTrack( 1, mSurface1 );
				}
			}
		}

		if( 0 > ret )
		{
			if( ASING_RUN )
			{
				PlayerNextFile();
			}
			else
			{
			}			

			FUNCOUT();
			return;
		}

		if( mDisplayMode == 0 ) {
			if( mSurface1 != null ) {
				if( PlayerActivity.mSuspendFlag ) {
					Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(PlayerActivity.mSavedCurtPos) + "ms");
					mMoviePlayer.Play();
					mMoviePlayer.Seek( (int)PlayerActivity.mSavedCurtPos);
					PlayerActivity.mSuspendFlag = false;
				}
				else {
					mMoviePlayer.Play();
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}
		else if( mDisplayMode == 1 ){
			if( mSurface2 != null ) {
				if( PlayerActivity.mSuspendFlag ) {
					Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(PlayerActivity.mSavedCurtPos) + "ms");
					mMoviePlayer.Play();
					mMoviePlayer.Seek( (int)PlayerActivity.mSavedCurtPos);
					PlayerActivity.mSuspendFlag = false;
				}
				else {
					mMoviePlayer.Play();
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}
		else {
			if( mSurface1 != null && mSurface2 != null )
			{
				if( mMoviePlayer.IsPlay() == false ) {
					if( PlayerActivity.mSuspendFlag ) {
						Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(PlayerActivity.mSavedCurtPos) + "ms");
						mMoviePlayer.Play();
						mMoviePlayer.Seek( (int)PlayerActivity.mSavedCurtPos);
						PlayerActivity.mSuspendFlag = false;
					}
					else {
						mMoviePlayer.Play();
					}
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}
			}		
		}
		
		FUNCOUT();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		FUNCIN();

		mMoviePlayer.Stop();
		mMoviePlayer.Close();
		
		if( mSurfaceHolder1 == holder ) {
			mSurfaceHolder1.removeCallback(this);
			mSurface1 = null;
		}
		if( mSurfaceHolder2 == holder ) {
			mSurfaceHolder2.removeCallback(this);
			mSurface2 = null;
		}

		FUNCOUT();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		FUNCIN();

		if( DEBUG_RENDERER )
		{
			TextView tv1 = (TextView)findViewById( R.id.textView1 );
			tv1.setTextSize( 30 );
			tv1.setTextColor( Color.WHITE );
			tv1.setAlpha( 10 );
			tv1.setText( "Secondary Display - " + (RENDER_VIDEOLAYER ? "Direct MLC Control" : "Surface Control") );

		}

		if( DEBUG_MEDIAINFO )
		{
			TextView tv2 = (TextView)findViewById( R.id.textView2 );
			String mediaInfo = mMoviePlayer.GetMediaInfo();
			tv2.setTextColor( Color.WHITE );
			tv2.setText( mediaInfo );
		}

		if( mVideoTrackNum == 0 ) {
			FUNCOUT();
			return;
		}
		
		LayoutParams lpPos1 = mSurfaceView1.getLayoutParams();
		LayoutParams lpPos2 = mSurfaceView2.getLayoutParams();
		
		switch( mDisplayMode )
		{
		case 0 :
			lpPos1.width	= GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
			lpPos1.height	= GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
			
			mSurfaceView1.setLayoutParams( lpPos1 );
			break;
		case 1 :
			lpPos2.width	= GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) );
			lpPos2.height	= GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) );

			mSurfaceView2.setLayoutParams( lpPos2 );
			break;
		case 2 :
			// Pip-view Case
			lpPos1.width	= GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
			lpPos1.height	= GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(0), mMoviePlayer.GetVideoHeight(0) );
			lpPos2.width	= GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) ) / 2;
			lpPos2.height	= GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) ) / 2;

			mSurfaceView1.setLayoutParams( lpPos1 );
			mSurfaceView2.setLayoutParams( lpPos2 );
			break;
		}
		
		FUNCOUT();
	}

	private int GetAspectRatioWidth( int width, int height )
	{
		int dspWidth = 0;

		double xRatio = (double)mScreenWidth / (double)width;
		double yRatio = (double)mScreenHeight / (double)height;

		if( xRatio > yRatio ) {
			dspWidth = (int)(width * yRatio);
		}
		else {
			dspWidth = mScreenWidth;	
		}

		return dspWidth;
	}

	private int GetAspectRatioHeight( int width, int height )
	{
		int dspHeight = 0;

		double xRatio = (double)mScreenWidth / (double)width;
		double yRatio = (double)mScreenHeight / (double)height;

		if( xRatio > yRatio ) {
			dspHeight = mScreenHeight;
		}
		else {
			dspHeight = (int)(height * xRatio);
		}

		return dspHeight;
	}

	public void PlayerPrevFile()
	{		
		mVideoName = ((MainActivity)MainActivity.mContext).GetPrevFileName();
		mDisplayMode = 0;
		
		SetDisplayLayout();
	}

	public void PlayerNextFile()
	{
		mVideoName = ((MainActivity)MainActivity.mContext).GetNextFileName();
		mDisplayMode = 0;
		
		SetDisplayLayout();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Implemetation Fucntion
	//
	public void SetDisplayLayout()
	{
		// Surface Close
		mSurfaceView1.setVisibility( SurfaceView.INVISIBLE );
		mSurfaceView2.setVisibility( SurfaceView.INVISIBLE );

		LayoutParams lpPos1 = mSurfaceView1.getLayoutParams();
		LayoutParams lpPos2 = mSurfaceView2.getLayoutParams();

		RelativeLayout.LayoutParams lpAlign1 = (RelativeLayout.LayoutParams)mSurfaceView1.getLayoutParams();
		RelativeLayout.LayoutParams lpAlign2 = (RelativeLayout.LayoutParams)mSurfaceView2.getLayoutParams();
		
		switch( mDisplayMode )
		{
		case 0 :
			lpPos1.width	= mScreenWidth;		lpPos1.height	= mScreenHeight;
			lpPos2.width	= 0;				lpPos2.height	= 0;
			
			lpAlign1.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			break;
		case 1 :
			lpPos1.width	= 0;				lpPos1.height	= 0;
			lpPos2.width	= mScreenWidth;		lpPos2.height	= mScreenHeight;
			
			lpAlign1.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			break;
		case 2 :
			// Pip-view Case
			lpPos1.width	= mScreenWidth;		lpPos1.height	= mScreenHeight;
			lpPos2.width	= mScreenWidth / 2;	lpPos2.height	= mScreenHeight / 2;
			
			lpAlign1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.id.playerLayOut);
			break;
		}
		
		mSurfaceView1.setLayoutParams( lpPos1 );
		mSurfaceView2.setLayoutParams( lpPos2 );

		mSurfaceView1.setLayoutParams( lpAlign1 );
		mSurfaceView2.setLayoutParams( lpAlign2 );
		
		mSurfaceHolder1 = mSurfaceView1.getHolder();
		mSurfaceHolder1.addCallback( this );

		mSurfaceHolder2 = mSurfaceView2.getHolder();
		mSurfaceHolder2.addCallback( this );
		
		if( mDisplayMode == 0 || mDisplayMode == 2 ) {
			mSurfaceView1.setVisibility( SurfaceView.VISIBLE );
			mSurfaceView1.setZOrderMediaOverlay( false );
		}
		if( mDisplayMode == 1 || mDisplayMode == 2 ) {
			mSurfaceView2.setVisibility( SurfaceView.VISIBLE );
			mSurfaceView2.setZOrderMediaOverlay( true );
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Log Message : Function IN / OUT 
	//
	private void FUNCIN() {
		// java.lang.Exception e = new java.lang.Exception();
		// StackTraceElement trace[] = e.getStackTrace();
		// Log.v( DBG_TAG, trace[1].getMethodName() + "()++" );
	}

	private void FUNCOUT() {
		// java.lang.Exception e = new java.lang.Exception();
		// StackTraceElement trace[] = e.getStackTrace();
		// Log.v( DBG_TAG, trace[1].getMethodName() + "()--" );
	}
}
