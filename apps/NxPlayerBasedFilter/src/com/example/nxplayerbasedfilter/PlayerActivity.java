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

import android.app.Activity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.View;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.content.Intent;
import android.view.MotionEvent;
import android.view.KeyEvent;

import android.widget.MediaController;

import android.view.Display;
import android.graphics.Point;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.ActionBar;

import android.os.Message;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

import android.graphics.Rect;
import android.graphics.RectF;

import android.hardware.display.DisplayManager;

import android.widget.TextView;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback, MediaController.MediaPlayerControl {
	private static final String DBG_TAG 		= "PlayerActivity";

	private static final boolean RENDER_VIDEOLAYER		= false;
	private static final boolean RENDOER_VIDEOLAYER_CRON= false;

	private static final boolean DEBUG_RENDERER			= true;
	private static final boolean DEBUG_MEDIAINFO		= true;

	private static final boolean ASING_RUN				= false;
	private static final boolean AGING_SEEK				= false;

	private static final String MSG_KEY					= "MSG_KEY";
	private static final int 	MSG_KEY_EOS				= 1;
	private static final int 	MSG_KEY_ERROR			= 2;
	private static final int	MSG_KEY_SHOWBAR			= 3;
	private static final int 	MSG_KEY_HIDEBAR			= 4;

	private static int 		mVisibleTime	= 5000; 
	
	public static boolean	mExitFlag		= false;
	public static boolean	mSuspendFlag	= false;
	public static long		mSavedCurtPos	= 0;
	
	public static Context	mContext;

	Bitmap			mBmpImage;
	int				mBmpWidth;
	int				mBmpHeight;
	
	RelativeLayout	mLayOut;
	ActionBar		mActionBar;
	ContextView		mContextView;
	
	Surface			mSurface1;
	SurfaceView		mSurfaceView1;
	SurfaceHolder	mSurfaceHolder1;

	Surface			mSurface2;
	SurfaceView		mSurfaceView2;
	SurfaceHolder	mSurfaceHolder2;

	MoviePlayer		mMoviePlayer;
	AudioManager	mAudioManager;
	MediaController mMediaController;
 	
	String			mVideoName;
	
	int				mVideoTrackNum;
	int				mAudioTrackNum;

	boolean			mMoveEvent;
	int				mMovePreviousPosX;
	int				mMovePreviousPosY;
	
	int				mDisplayMode = 0; 
	
	int				mDisplayModeChange;
	int				mScreenWidth, mScreenHeight;
	int				mVidWidth, mVidHeight;
	int				mScreenRotate;
	
	private Handler	mHandler;
	private PlayerPresentation mPlayerPresentation = null;
	
	SeekTestThread	mSeekTest;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Activity Override Function
	//
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		FUNCIN();
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_player );

		// TODO Auto-generated method stub
		mLayOut 		= (RelativeLayout)findViewById( R.id.playerLayOut );
		mContext 		= this;
		mActionBar		= getActionBar();
		mContextView	= (ContextView)findViewById( R.id.contextView );
		
		mSurfaceView1 	= (SurfaceView)findViewById( R.id.surfaceView1 );
		mSurfaceView2	= (SurfaceView)findViewById( R.id.surfaceView2 );
		mAudioManager 	= (AudioManager)getSystemService( AUDIO_SERVICE );
		
		mVideoName 		= ((MainActivity)MainActivity.mContext).GetFileName();

		// Bitmap image load
		Resources res = getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();
		
		mBmpImage = BitmapFactory.decodeResource(res, R.drawable.audio_only, options);
		mBmpWidth = options.outWidth;
		mBmpHeight = options.outHeight;
		//Log.v(DBG_TAG, "bmp width(" + String.valueOf(mBmpWidth) +"), bmp height(" + String.valueOf(mBmpHeight) + ")");

		// Get Display Information
		Display display	= ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point point = new Point();
		display.getRealSize( point );
		
		mScreenWidth	= point.x;
		mScreenHeight	= point.y;
		mScreenRotate	= display.getRotation();
		//Log.v(DBG_TAG, "screen width(" + String.valueOf(mScreenWidth) +"), screen height(" + String.valueOf(mScreenHeight) + "), screen rotate(" + String.valueOf(mScreenRotate) +")");
		
		mLayOut.setBackgroundColor( Color.rgb( 0x00,0x00,0x00 ) );
		mMoveEvent = false;
		
		// UI Change Handler
		mHandler = new ExternReferenceHandler( this ); 

		// Register Media Controller
		mMediaController = new MediaController(this);
		mMediaController.setMediaPlayer(this);
		mMediaController.setAnchorView( (MediaController)findViewById(R.id.mediaController1) );
		mMediaController.setPrevNextListeners( mNextBtn, mPrevBtn );

		mMoviePlayer = MoviePlayer.GetInstance();

		if( AGING_SEEK )
		{
			mSeekTest = new SeekTestThread();
		}
		
		// Static function call ( DUMMY ) -> PDK Optimize Bug
		// Do not delete! (Calling in JNI)
		EventHandler( 0xFFFF, 0x0000 );

		FUNCOUT();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//return super.onCreateOptionsMenu(menu);
		
		getMenuInflater().inflate(R.menu.player, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		if (id == R.id.player_displaysetting) {
			mMoviePlayer.Pause();
			
			AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
			alertDlg.setTitle( "Display Setting" );
			
			if( mVideoTrackNum == 2 ) {
				final String items[] = { "Front view", "Rear view", "Dual view" };

				alertDlg.setSingleChoiceItems(items, mDisplayMode, 
						new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							mDisplayModeChange = whichButton;
						}
						}).setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
							if( mDisplayMode == mDisplayModeChange ) {
								mMoviePlayer.Play();
								return;
							}
							
							mDisplayMode = mDisplayModeChange;
							
							mExitFlag = false;
							mSuspendFlag = true;

							mSavedCurtPos = mMoviePlayer.GetPosition();
							Log.v(DBG_TAG, "Save CurrentPos = " + String.valueOf(mSavedCurtPos) );
							SetDisplayLayout();
						}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
							mMoviePlayer.Play();
						}
				});
			}
			else {
				alertDlg.setMessage( "Not support contents." );
				alertDlg.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int which ) {
						dialog.dismiss();
						mMoviePlayer.Play();
					}
				});
			}
			
			alertDlg.show();
			
			return true;
		} else if (id == R.id.player_mediainfo) {
			mMoviePlayer.Pause();
			
			AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
			alertDlg.setTitle( "Media Infomation" );
			String mediaInfo = mMoviePlayer.GetMediaInfo();
			
			alertDlg.setMessage( mediaInfo );
			alertDlg.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
			    @Override
			    public void onClick( DialogInterface dialog, int which ) {
			        dialog.dismiss();
			        mMoviePlayer.Play();
			    }
			});
			alertDlg.show();
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		FUNCIN();
		
		// TODO Auto-generated method stub
		if( !((MainActivity)MainActivity.mContext).IsTVOut() )
		{
			SetDisplayLayout();
		}
		else
		{
			DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
			Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
			if (presentationDisplays.length > 0) {
				Display dsp = presentationDisplays[0];
				mPlayerPresentation = new PlayerPresentation(this, dsp);
				mPlayerPresentation.show();
			}
		}			
	
		super.onResume();
		FUNCOUT();
	}
	
	@Override
	protected void onPause() {
		FUNCIN();
		
		// TODO Auto-generated method stub
		if( !mExitFlag ) {
			mSavedCurtPos = mMoviePlayer.GetPosition();
			Log.v(DBG_TAG, "Save CurrentPos = " + String.valueOf(mSavedCurtPos) );
			mSuspendFlag = true;
			mExitFlag = false;
		}
		
		mActionBar.hide();
		mMediaController.hide();
		mContextView.Hide();

		mMoviePlayer.Stop();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if( ((MainActivity)MainActivity.mContext).IsTVOut() ) {
			mPlayerPresentation.dismiss();
			mPlayerPresentation = null;
		}

		super.onPause();
		FUNCOUT();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		int x = (int)event.getX();
		int y = (int)event.getY();
		
		int perX = (x * 100) / mScreenWidth;
		int perY = (y * 100) / mScreenHeight;

		switch(event.getAction())
		{
		case MotionEvent.ACTION_DOWN :
			mMovePreviousPosX = perX;
			mMovePreviousPosY = perY;
			break;
		case MotionEvent.ACTION_MOVE:
			if( mMovePreviousPosX != 0 && mMovePreviousPosY != 0 ) {
				int nMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				int nCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if( perX > 70 ) {
					if( mMovePreviousPosY > perY + 1 ) {
						mMovePreviousPosY = perY;
						
						nCurVolume += 1;
						if( nMaxVolume < nCurVolume ) nCurVolume = nMaxVolume;
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nCurVolume, 0);
						mMoveEvent = true;
					}
					if( mMovePreviousPosY < perY - 1 ) {
						mMovePreviousPosY = perY;
						
						nCurVolume -= 1;
						if( 0 > nCurVolume ) nCurVolume = 0;
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nCurVolume, 0);
						mMoveEvent = true;
					}
				}

				if( mMediaController.isShowing() == true ) 	mMediaController.show(mVisibleTime);
				if( mActionBar.isShowing() == true  ) 		mActionBar.show();
				if( mActionBar.isShowing() == true  )		mContextView.Show();
			}
			else {
				mMovePreviousPosX = perX;
				mMovePreviousPosY = perY;
			}
			break;
		case MotionEvent.ACTION_UP:
			if( mMoveEvent == false ) {
				if( mMediaController.isShowing() == false && mActionBar.isShowing() == false && mActionBar.isShowing() == false )	{
					mMediaController.show(mVisibleTime);
					mActionBar.show();
					mContextView.Show();
				}
				else {
					mActionBar.hide();
					mMediaController.hide();
					mContextView.Hide();
				}
			}
			mMoveEvent = false;
			break;
		default:
			break;
		}
		return true;
	}
	
	@Override
	public boolean dispatchKeyEvent( KeyEvent event )
	{
		int keyCode = event.getKeyCode();

		if( KeyEvent.KEYCODE_BACK == keyCode) {
			mExitFlag = true;
			PlayerExit();
		}
		return super.dispatchKeyEvent(event);		
	}

	@Override
	public boolean dispatchTouchEvent( MotionEvent event )
	{
		switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
		 		break;
		 	case MotionEvent.ACTION_UP:
			 	break;
			 default:
			 	break;
		 }

		return super.dispatchTouchEvent(event);		
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
					AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
					alertDlg.setMessage( "Not support contents." );
					alertDlg.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int which ) {
							dialog.dismiss();
							PlayerNextFile();
						}
					});
					alertDlg.show();
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
						String strTemp = "Not Support Contents.";
						if( -3 == ret )
							strTemp = "Not Support Audio Codec.";

						AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
						alertDlg.setMessage( strTemp );
						alertDlg.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
							@Override
							public void onClick( DialogInterface dialog, int which ) {
								dialog.dismiss();
								PlayerNextFile();
							}
						});
						alertDlg.show();
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
					
					if( mMediaController.isShowing() )	mMediaController.show(mVisibleTime);
					if( mActionBar.isShowing() )		mActionBar.show();
					if( mContextView.isShowing() )		mContextView.Show();

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

					mMoviePlayer.AddVideoConfig( 0, DISPLAY_PORT.LCD, DISPLAY_MODULE.MLC0, srcRect, dstRect );
					ret = mMoviePlayer.AddVideoTrack( 0, null );
					mMoviePlayer.SetVideoLayerPriority( 0, DISPLAY_MODULE.MLC0, 1 );

					if( RENDOER_VIDEOLAYER_CRON )
					{
						dstRect.x		= 0;
						dstRect.y		= 0;
						dstRect.width	= 1920;
						dstRect.height	= 1080;
						mMoviePlayer.AddSubDisplay( 0, DISPLAY_PORT.HDMI, DISPLAY_MODULE.MLC1, srcRect, dstRect );
						mMoviePlayer.SetVideoLayerPriority( 0, DISPLAY_MODULE.MLC1, 1 );
					}
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
					mVidWidth = GetAspectRatioWidth( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) );
					mVidHeight = GetAspectRatioHeight( mMoviePlayer.GetVideoWidth(1), mMoviePlayer.GetVideoHeight(1) );

					NX_CDisplayRect srcRect = new NX_CDisplayRect();
					NX_CDisplayRect dstRect = new NX_CDisplayRect();
					
					dstRect.x		= mScreenWidth / 2 - mVidWidth / 2;
					dstRect.y		= mScreenHeight / 2 - mVidHeight / 2;
					dstRect.width	= mVidWidth;
					dstRect.height	= mVidHeight;

					mMoviePlayer.AddVideoConfig( 1, DISPLAY_PORT.LCD, DISPLAY_MODULE.MLC0, srcRect, dstRect );
					ret = mMoviePlayer.AddVideoTrack( 1, null );
					mMoviePlayer.SetVideoLayerPriority( 1, DISPLAY_MODULE.MLC0, 1 );

					if( RENDOER_VIDEOLAYER_CRON )
					{
						dstRect.x		= 0;
						dstRect.y		= 0;
						dstRect.width	= 1920;
						dstRect.height	= 1080;
						mMoviePlayer.AddSubDisplay( 1, DISPLAY_PORT.HDMI, DISPLAY_MODULE.MLC1, srcRect, dstRect );
						mMoviePlayer.SetVideoLayerPriority( 1, DISPLAY_MODULE.MLC1, 1 );
					}
				}
				else
				{
					ret = mMoviePlayer.AddVideoTrack( 1, mSurface2 );	
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
				String strTemp = "Not Support Contents.";
				if( -3 == ret )
					strTemp = "Not Support Audio Codec.";
				else if( -4 == ret )
					strTemp = "Not Support Video Codec.";
				else if( -5 == ret || -6 == ret )
					strTemp = "Not Support Video Size";

				AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
				alertDlg.setMessage( strTemp );
				alertDlg.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int which ) {
						dialog.dismiss();
						PlayerNextFile();
					}
				});
				alertDlg.show();
			}				

			FUNCOUT();
			return;
		}

		if( mDisplayMode == 0 ) {
			if( mSurface1 != null ) {
				if( mSuspendFlag ) {
					Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(mSavedCurtPos) + "ms");
					mMoviePlayer.Play();
					mMoviePlayer.Seek( (int)mSavedCurtPos);
					mSuspendFlag = false;
				}
				else {
					mMoviePlayer.Play();
					
					if( AGING_SEEK )
					{
						mSeekTest.start();
					}
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				if( mMediaController.isShowing() ) 	mMediaController.show( mVisibleTime );
				if( mActionBar.isShowing() )		mActionBar.show();
				if( mContextView.isShowing() )		mContextView.Show();
			}
		}
		else if( mDisplayMode == 1 ) {
			if( mSurface2 != null ) {
				if( mSuspendFlag ) {
					Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(mSavedCurtPos) + "ms");
					mMoviePlayer.Play();
					mMoviePlayer.Seek( (int)mSavedCurtPos);
					mSuspendFlag = false;
				}
				else {
					mMoviePlayer.Play();
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				if( mMediaController.isShowing() )	mMediaController.show( mVisibleTime );
				if( mActionBar.isShowing() )		mActionBar.show();
				if( mContextView.isShowing() )		mContextView.Show();
			}
		}
		else {
			if( mSurface1 != null && mSurface2 != null )
			{
				if( mMoviePlayer.IsPlay() == false ) {
					if( mSuspendFlag ) {
						Log.v(DBG_TAG, "Resume Player. Seek to " + String.valueOf(mSavedCurtPos) + "ms");
						mMoviePlayer.Play();
						mMoviePlayer.Seek( (int)mSavedCurtPos);
						mSuspendFlag = false;
					}
					else {
						mMoviePlayer.Play();
					}
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					if( mMediaController.isShowing() )	mMediaController.show(mVisibleTime);
					if( mActionBar.isShowing() )		mActionBar.show();
					if( mContextView.isShowing() )		mContextView.Show();
				}
			}		
		}
		
		FUNCOUT();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		FUNCIN();

		if( AGING_SEEK )
		{
			if( mSeekTest.isAlive() ){
				mSeekTest.interrupt();
				try {  
					mSeekTest.join();
				} catch(InterruptedException e) {
				}
			}
		}		
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
			tv1.setText( "Primary Display - " + (RENDER_VIDEOLAYER ? "Direct MLC Control" : "Surface Control") );
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

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Control Function
	//
	@Override
	public boolean canPause() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canSeekForward() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		// TODO Auto-generated method stub
		// FUNCIN();
		// FUNCOUT();
		return (int)mMoviePlayer.GetPosition();
	}

	@Override
	public int getDuration() {
		// TODO Auto-generated method stub
		// FUNCIN();
		// FUNCOUT();
		return (int)mMoviePlayer.GetDuration();
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		// FUNCIN();
		// FUNCOUT();
		return mMoviePlayer.IsPlay();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		// FUNCIN();
		
		mMoviePlayer.Pause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// FUNCOUT();
	}

	@Override
	public void seekTo(int arg0) {
		// TODO Auto-generated method stub
		// FUNCIN();

		mMoviePlayer.Seek( arg0 );
		
		// FUNCOUT();
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		// FUNCIN();

		mMoviePlayer.Play();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// FUNCOUT();
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Event Listener
	//
	View.OnClickListener mPrevBtn = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			PlayerActivity.this.PlayerPrevFile();
		}
	};

	View.OnClickListener mNextBtn = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			PlayerActivity.this.PlayerNextFile();
		}
	};


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
	
	public void PlayerPrevFile()
	{
		FUNCIN();
		
		if( !((MainActivity)MainActivity.mContext).IsTVOut() ) {
			mVideoName = ((MainActivity)MainActivity.mContext).GetPrevFileName();
			mDisplayMode = 0;
			SetDisplayLayout();
		}
		else {
			mPlayerPresentation.PlayerPrevFile();
		}

		FUNCOUT();
	}

	public void PlayerNextFile()
	{
		FUNCIN();
	
		if( !((MainActivity)MainActivity.mContext).IsTVOut() ) {
			mVideoName = ((MainActivity)MainActivity.mContext).GetNextFileName();
			mDisplayMode = 0;
			SetDisplayLayout();
		}
		else {
			mPlayerPresentation.PlayerNextFile();
		}

		FUNCOUT();
	}

	public void PlayerExit()
	{
		FUNCIN();

		if( ((MainActivity)MainActivity.mContext).IsTVOut() )
			mPlayerPresentation.dismiss();

		Intent intent = new Intent( PlayerActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
		FUNCOUT();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Inner Handler Class
	//	
	private static class ExternReferenceHandler extends Handler {
		PlayerActivity mActivity;
		
		public ExternReferenceHandler( PlayerActivity activity ) {
			this.mActivity = activity;
		}
		
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Bundle bundle = msg.getData();
			int msgKey = bundle.getInt( MSG_KEY );
			
			if( msgKey == MSG_KEY_EOS ) {
				if( ((MainActivity)MainActivity.mContext).IsNetworkStream() ) 
					mActivity.PlayerExit();
				else
					mActivity.PlayerNextFile();
			}
			else if( msgKey == MSG_KEY_ERROR ) {
				mActivity.PlayerExit();
			}
			else if( msgKey == MSG_KEY_SHOWBAR ) {
				mActivity.mMediaController.show(mVisibleTime);
				mActivity.mActionBar.show();
			}
			else if( msgKey == MSG_KEY_HIDEBAR ) {
				//Log.v(DBG_TAG, "Hide Bar!!");
			}
		}	
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Implemetation ContextView ( Cusrtom View )
	//	
	public static class ContextView extends View implements View.OnSystemUiVisibilityChangeListener {
		//private static final String DBG_TAG = "ContentView";
		int mLastSystemUiVis;
		boolean mVisible;

		Runnable mNaviHiderTask = new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				setNavVisibility(false);
				mVisible = false;
			}
		};

		public ContextView(Context context, AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
			setOnSystemUiVisibilityChangeListener( this );
			setNavVisibility( false );
		}
		
		void setNavVisibility(boolean visible) {
			int visibility = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | SYSTEM_UI_FLAG_LAYOUT_STABLE;
			
			if( !visible ) {
				visibility |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			}

			setSystemUiVisibility( visibility );
		}
		
		@Override
		public void onSystemUiVisibilityChange(int visibility) {
			// TODO Auto-generated method stub
			int diff = mLastSystemUiVis ^ visibility;
			mLastSystemUiVis = visibility;

			if( (diff & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 && (visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0 ) {
				setNavVisibility( true );
                Handler handler = getHandler();
                if( handler != null ) {
                	handler.removeCallbacks( mNaviHiderTask );
                	handler.postDelayed( mNaviHiderTask, mVisibleTime );
                	mVisible = true;
                }				
				
                Bundle bundle = new Bundle();
				bundle.putInt( MSG_KEY, MSG_KEY_SHOWBAR );
				Message msg = ((PlayerActivity)PlayerActivity.mContext).mHandler.obtainMessage();
				msg.setData(bundle);
				((PlayerActivity)PlayerActivity.mContext).mHandler.sendMessage(msg);
			}
		}

		public void Show()
		{
            Handler handler = getHandler();
            if( handler != null ) {
            	handler.removeCallbacks( mNaviHiderTask );
            	handler.postDelayed( mNaviHiderTask, mVisibleTime );
            	mVisible = true;
            }				
		}
		
		public void Hide()
		{
            Handler handler = getHandler();
            if( handler != null ) {
            	handler.removeCallbacks( mNaviHiderTask );
            	handler.postDelayed( mNaviHiderTask, 0 );
            	mVisible = false;
            }				
		}

		public boolean isShowing()
		{
			return mVisible;
		}
	}

	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Thumbnail Thread
	//	
	class SeekTestThread extends Thread {
		public void run() {
			FUNCIN();
			
			long duration = mMoviePlayer.GetDuration();
			long position; 
			int seekMargine = 10000;
			int seekUnit = 50000;
			
			while( true )
			{
				if( Thread.currentThread().isInterrupted() )
					break;
				
				this.sleep(5000);
				
				position = mMoviePlayer.GetPosition();
				if( position + seekUnit + seekMargine > duration )
				{
					position = 10000;
				}
				else
				{
					position += seekUnit;
				}
				
				Log.i(DBG_TAG, "Seek to " + String.valueOf(position) + " ms");
				mMoviePlayer.Seek((int)position);
			}

			FUNCOUT();
		}
		
		public void sleep(int time){
		    try {
		      Thread.sleep(time);
		    } catch (InterruptedException e) { }
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


	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// EventHandler by calling JNI
	//
	static void EventHandler( int eventType, int eventData )
	{
		if( 0x1000 == eventType ) {
			Log.v(DBG_TAG, "End of stream.");
			
			if( ((PlayerActivity)PlayerActivity.mContext).mHandler != null) {
				Bundle bundle = new Bundle();
				bundle.putInt( MSG_KEY, MSG_KEY_EOS );

				Message msg = ((PlayerActivity)PlayerActivity.mContext).mHandler.obtainMessage();
				msg.setData(bundle);
				((PlayerActivity)PlayerActivity.mContext).mHandler.sendMessage(msg);
			}
		}
		else if( 0x8001 == eventType ){
			Log.v(DBG_TAG, "Cannot play contents.");
			Bundle bundle = new Bundle();
			
			bundle.putInt( MSG_KEY, MSG_KEY_ERROR );
			Message msg = ((PlayerActivity)PlayerActivity.mContext).mHandler.obtainMessage();
			msg.setData(bundle);
			((PlayerActivity)PlayerActivity.mContext).mHandler.sendMessage(msg);
		}
	}
}
