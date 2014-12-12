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
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.content.Intent;

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

public class PlayerActivity extends Activity implements SurfaceHolder.Callback, MediaController.MediaPlayerControl {
	private static final String DBG_TAG 		= "NxPlayerBasedFilter.PlayerActivity";

	private static final String MSG_KEY			= "MSG_KEY";
	private static final int 	MSG_KEY_EOS		= 1;
	private static final int 	MSG_KEY_ERROR	= 2;
	private static final int	MSG_KEY_SHOWBAR	= 3;
	private static final int 	MSG_KEY_HIDEBAR	= 4;
	
	private static final int ASPECT_RATIO_WIDTH		= 16;
	private static final int ASPECT_RATIO_HEIGHT	= 9;
	
	private static int mVisibleTime = 5000; 
	
	private static boolean	mPauseFlag;
	private static int 		mPlayerCurrentPos;
	
	public static Context mContext;

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
	String			mMediaInfo;
	
	int				mVideoTrackTotalNum;
	int				mVideoTrackRequest = 1;
	
	boolean			mMoveEvent;
	int				mMovePreviousPos;
	
	int				mDisplayMode = 0; 
	int				mDisplayModeChange;
	int				mScreenWidth, mScreenHeight;
	int				mScreenRotate;
	
	private Handler	mHandler;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Activity Override Function
	//
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.v(DBG_TAG, "onCreate()++");
		
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
		Resources res = mContext.getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();
		
        mBmpImage = BitmapFactory.decodeResource(res, R.drawable.audio_only, options);
		mBmpWidth = options.outWidth;
		mBmpHeight = options.outHeight;
		//Log.v(DBG_TAG, "bmp width(" + String.valueOf(mBmpWidth) +"), bmp height(" + String.valueOf(mBmpHeight) + ")");
        
		// Get Display Information
		Display display	= ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point point = new Point();
		display.getRealSize( point );
		
		mScreenWidth	= point.x;
		mScreenHeight	= point.y;
		mScreenRotate	= display.getRotation();
		//Log.v(DBG_TAG, "screen width(" + String.valueOf(mScreenWidth) +"), screen height(" + String.valueOf(mScreenHeight) + "), screen rotate(" + String.valueOf(mScreenRotate) +")");
		
		SetDisplayLayout();

		mLayOut.setBackgroundColor( Color.rgb(0,0,0) );
		mMoveEvent = false;
		
		// UI Change Handler
		mHandler = new ExternReferenceHandler( this ); 

		// Register Media Controller
		mMediaController = new MediaController(this);
		mMediaController.setMediaPlayer(this);
		mMediaController.setAnchorView( (MediaController)findViewById(R.id.mediaController1) );
		mMediaController.setPrevNextListeners( mNextBtn, mPrevBtn );

		// Static function call ( DUMMY )
		// Do not delete! (Calling in JNI)
		EventHandler( 0xFFFF, 0x0000 );

		//Log.v(DBG_TAG, "onCreate()--");
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
			
			if( mVideoTrackTotalNum == 2 ) {
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
							mVideoTrackRequest = mDisplayModeChange + 1;
							
							mPlayerCurrentPos = mMoviePlayer.GetCurPosition();
							Log.v(DBG_TAG, "Save CurrentPos = " + String.valueOf(mPlayerCurrentPos) );

							PlayerActivity.mPauseFlag = true;
							PlayerActivity.this.SetDisplayLayout();
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
			mMediaInfo = mMoviePlayer.GetMediaInfo();
			
			alertDlg.setMessage( mMediaInfo );
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
	protected void onDestroy() {
		//Log.v(DBG_TAG, "onDestroy()++");
		
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if( mMoviePlayer != null ) {
			mMoviePlayer.Close();
			mMoviePlayer = null;
		}

		//Log.v(DBG_TAG, "onDestroy()--");
	}
	
	@Override
	protected void onStart() {
		//Log.v(DBG_TAG, "onStart()++");
		
		// TODO Auto-generated method stub
		super.onStart();
		
		//Log.v(DBG_TAG, "onStart()--");
	}
	
	@Override
	protected void onRestart() {
		//Log.v(DBG_TAG, "onRestart()++");
		
		// TODO Auto-generated method stub
		super.onRestart();
		
		//Log.v(DBG_TAG, "onRestart()--");
	}
	
	@Override
	protected void onStop() {
		//Log.v(DBG_TAG, "onStop()++");
		
		// TODO Auto-generated method stub
		super.onStop();
		
		//Log.v(DBG_TAG, "onStop()--");
	}
	
	@Override
	protected void onResume() {
		//Log.v(DBG_TAG, "onResume()++");
		
		// TODO Auto-generated method stub
		super.onResume();
		
		SetDisplayLayout();
		
		//Log.v(DBG_TAG, "onResume()--");
	}
	
	@Override
	protected void onPause() {
		//Log.v(DBG_TAG, "onPause()++");
		
		// TODO Auto-generated method stub
		super.onPause();
	
		if( mMoviePlayer != null ) {
			mPlayerCurrentPos = mMoviePlayer.GetCurPosition();
			Log.v(DBG_TAG, "Save CurrentPos = " + String.valueOf(mPlayerCurrentPos) );
			
			mMoviePlayer.Stop();
			mMoviePlayer.Close();
			mMoviePlayer = null;
		}
		
		mPauseFlag = true;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		//Log.v(DBG_TAG, "onPause()--");
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "onBackPressed()++");
		//super.onBackPressed();
		
		if( mMoviePlayer != null ) {
			mMoviePlayer.Stop();
			mMoviePlayer.Close();
			mMoviePlayer = null;
		}
		
		PlayerExit();
		//Log.v(DBG_TAG, "onBackPressed()--");
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
			mMovePreviousPos = perY;
			break;
		case MotionEvent.ACTION_MOVE:
			if( mMovePreviousPos != 0) {
				int nMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				int nCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if( perX > 70 ) {
					if( mMovePreviousPos > perY + 1 ) {
						mMovePreviousPos = perY;
						
						nCurVolume += 1;
						if( nMaxVolume < nCurVolume ) nCurVolume = nMaxVolume;
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nCurVolume, 0);
						mMoveEvent = true;
					}
					if( mMovePreviousPos < perY - 1 ) {
						mMovePreviousPos = perY;
						
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
				mMovePreviousPos = perY;
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
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Surface Callback
	//
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "surfaceCreate()++");

		if( mSurface1 == null && mSurface2 == null ) {
			mMoviePlayer = MoviePlayer.GetInstance();
			mMoviePlayer.SetFileName( mVideoName );
			mVideoTrackTotalNum = mMoviePlayer.GetVideoTrackNum();
		}
		
		// Audio Case
		if( mVideoTrackTotalNum == 0 ) {
			mVideoTrackRequest = 0;

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
			canvas.drawRGB(0xFF,  0xFF,  0xFF);
			canvas.drawBitmap(mBmpImage, srcImage, dstImage, null);
			mSurfaceHolder1.unlockCanvasAndPost(canvas);
			
			mMoviePlayer.Open( null, null, mVideoTrackRequest, false );
			mMoviePlayer.Play();
			
			if( mMediaController.isShowing() ) mMediaController.show(mVisibleTime);
			if( mActionBar.isShowing() )		mActionBar.show();
			if( mContextView.isShowing() )		mContextView.Show();

			return;
		}
		
		// Video Case
		if( mSurfaceHolder1 == holder && mSurface1 == null ) {
			mSurface1 = mSurfaceHolder1.getSurface();
			
			if( mDisplayMode == 0 ) mMoviePlayer.Open( mSurface1, null, mVideoTrackRequest, false );
		}
		if( mSurfaceHolder2 == holder && mSurface2 == null ) {
			mSurface2 = mSurfaceHolder2.getSurface();
			
			if( mDisplayMode == 1 ) mMoviePlayer.Open( mSurface2, null, mVideoTrackRequest, false );
			else					mMoviePlayer.Open( mSurface1, mSurface2, mVideoTrackRequest, true);
		}

		if( mDisplayMode == 0 ) {
			if( mSurface1 != null ) {
				if( mPauseFlag ) {
					Log.v(DBG_TAG, "Dectect Pause Flag. Seek to " + String.valueOf(mPlayerCurrentPos) + "ms");
					mMoviePlayer.Seek(mPlayerCurrentPos);
					mMoviePlayer.Play();
					mPauseFlag = false;
				}
				else {
					mMoviePlayer.Play();
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				if( mMediaController.isShowing() ) 	mMediaController.show(mVisibleTime);
				if( mActionBar.isShowing() )		mActionBar.show();
				if( mContextView.isShowing() )		mContextView.Show();
			}
		}
		else if( mDisplayMode == 1 ){
			if( mSurface2 != null ) {
				if( mPauseFlag ) {
					Log.v(DBG_TAG, "Dectect Pause Flag. Seek to " + String.valueOf(mPlayerCurrentPos) + "ms");
					mMoviePlayer.Seek(mPlayerCurrentPos);
					mMoviePlayer.Play();
					mPauseFlag = false;
				}
				else {
					mMoviePlayer.Play();
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				if( mMediaController.isShowing() ) mMediaController.show(mVisibleTime);
				if( mActionBar.isShowing() )		mActionBar.show();
				if( mContextView.isShowing() )		mContextView.Show();
			}
		}
		else {
			if( mSurface1 != null && mSurface2 != null )
			{
				if( mMoviePlayer.IsPlay() == 0 ) {
					if( mPauseFlag ) {
						Log.v(DBG_TAG, "Dectect Pause Flag. Seek to " + String.valueOf(mPlayerCurrentPos) + "ms");
						mMoviePlayer.Seek(mPlayerCurrentPos);
						mMoviePlayer.Play();
						mPauseFlag = false;
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
		
		//Log.v(DBG_TAG, "surfaceCreate()--");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "surfaceDestroyed()++");
		
		if( mMoviePlayer != null ) {
			mMoviePlayer.Stop();
			mMoviePlayer.Close();
			mMoviePlayer = null;
		}
		
		if( mSurfaceHolder1 == holder ) {
			mSurfaceHolder1.removeCallback(this);
			mSurface1 = null;
		}
		if( mSurfaceHolder2 == holder ) {
			mSurfaceHolder2.removeCallback(this);
			mSurface2 = null;
		}
		//Log.v(DBG_TAG, "surfaceDestroyed()--");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "surfaceChanged()++");
		//Log.v(DBG_TAG, "surfaceChanged()--");
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
		//Log.v(DBG_TAG, "getCurrentPosition()++");
		
		int nCurPosition = 0;
		if( mMoviePlayer != null )
			nCurPosition = mMoviePlayer.GetCurPosition();
		
		//Log.v(DBG_TAG, "getCurrentPosition()--");
		return nCurPosition;
	}

	@Override
	public int getDuration() {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "getDuration()++");
		
		int nDuration = 0;
		if( mMoviePlayer != null )
			nDuration = mMoviePlayer.GetDuration();
		
		//Log.v(DBG_TAG, "getDuration()--");
		return nDuration;
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "isPlaying()++");
		
		boolean bPlay = false;
		if( mMoviePlayer != null )
			bPlay = (mMoviePlayer.IsPlay() == 1) ? true : false;
		
		//Log.v(DBG_TAG, "isPlaying()--");
		return bPlay;
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "pause()++");
		
		if( mMoviePlayer != null ) {
			mMoviePlayer.Pause();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		
		//Log.v(DBG_TAG, "pause()--");
	}

	@Override
	public void seekTo(int arg0) {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "seekTo()++");
		
		if( mMoviePlayer != null )
			mMoviePlayer.Seek( arg0 );
		
		//Log.v(DBG_TAG, "seekTo()--");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "start()++");
		
		if( mMoviePlayer != null ) {
			mMoviePlayer.Play();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		//Log.v(DBG_TAG, "start()--");
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Event Listener
	//
	View.OnClickListener mPrevBtn = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//Log.v(DBG_TAG, "Pressed Previous Button.");
			PlayerActivity.this.PlayerPrevFile();
		}
	};

	View.OnClickListener mNextBtn = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//Log.v(DBG_TAG, "Pressed Next Button.");
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
		
		// Multi-view Case
		//int dspWidth	= (mDisplayMode == 2) ? mScreenWidth / 2 : mScreenWidth;
		
		int dspWidth	= mScreenWidth;
		int dspHeight	= mScreenHeight;

		double xRatio = (double)dspWidth / (double)ASPECT_RATIO_WIDTH;
		double yRatio = (double)dspHeight / (double)ASPECT_RATIO_HEIGHT;

		if( xRatio > yRatio )	dspWidth = (int)(ASPECT_RATIO_WIDTH * yRatio);
		else					dspHeight = (int)(ASPECT_RATIO_HEIGHT * xRatio);
		
		switch( mDisplayMode )
		{
		case 0 :
			lpPos1.width	= dspWidth;	lpPos1.height	= dspHeight;
			lpPos2.width	= 0;		lpPos2.height	= 0;
			
			lpAlign1.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			break;
		case 1 :
			lpPos1.width	= 0;		lpPos1.height	= 0;
			lpPos2.width	= dspWidth;	lpPos2.height	= dspHeight;
			
			lpAlign1.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_HORIZONTAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			break;
		case 2 :
			// Multi-view Case
			//lpPos1.width	= dspWidth;	lpPos1.height	= dspHeight;
			//lpPos2.width	= dspWidth;	lpPos2.height	= dspHeight;
			
			//lpAlign1.addRule(RelativeLayout.ALIGN_PARENT_LEFT, R.id.playerLayOut);
			//lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			//lpAlign2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.playerLayOut);
			//lpAlign2.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			
			// Pip-view Case
			lpPos1.width	= dspWidth;		lpPos1.height	= dspHeight;
			lpPos2.width	= dspWidth / 2;	lpPos2.height	= dspHeight / 2;
			
			lpAlign1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.playerLayOut);
			lpAlign1.addRule(RelativeLayout.CENTER_VERTICAL, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.playerLayOut);
			lpAlign2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.id.playerLayOut);
			
			mSurfaceView1.setZOrderMediaOverlay( false );
			mSurfaceView2.setZOrderMediaOverlay( true );
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
		
		// .Warnning
		// SurfaceHolder.setType
		// This method was deprecated in API level 11.
		// this is ignored, this value is set automatically when needed.
		//mSurfaceHolder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//mSurfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mSurfaceView1.setVisibility( SurfaceView.VISIBLE );
		mSurfaceView2.setVisibility( SurfaceView.VISIBLE );
	}
	
	public void PlayerPrevFile()
	{
		mVideoName = ((MainActivity)MainActivity.mContext).GetPrevFileName();
		mDisplayMode = 0;
		mVideoTrackRequest = mDisplayMode + 1;
		SetDisplayLayout();
	}

	public void PlayerNextFile()
	{
		mVideoName = ((MainActivity)MainActivity.mContext).GetNextFileName();
		mDisplayMode = 0;
		mVideoTrackRequest = mDisplayMode + 1;
		SetDisplayLayout();
	}

	public void PlayerExit()
	{
		mPauseFlag = false;
		Intent intent = new Intent( PlayerActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
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
			//Log.v(DBG_TAG, "ContentView()++");
			setOnSystemUiVisibilityChangeListener( this );
			setNavVisibility( false );
			
			//Log.v(DBG_TAG, "ContentView()--");
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
	// EventHandler by calling JNI
	//
	static void EventHandler( int eventType, int eventData )
	{
		//Log.v(DBG_TAG, "EventHandler() : type(" + String.valueOf(eventType)	+ "), data(" + String.valueOf(eventData) + ")" );
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
		//Log.v(DBG_TAG, "EventHandler()--");
	}
}
