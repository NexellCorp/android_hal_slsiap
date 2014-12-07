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

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.util.Log;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.view.View;

import android.provider.MediaStore;
import android.database.Cursor;
import java.util.ArrayList;

import android.content.Intent;
import android.content.Context;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.res.Resources;

import android.app.ListActivity;
import android.view.LayoutInflater;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import android.graphics.drawable.Drawable;
import android.widget.RelativeLayout;

public class MainActivity extends ListActivity {
	private static final boolean SUPPORT_THUMBNAIL = false;
	
	private static final String DBG_TAG = "NxPlayerBasedFilter.MainActivity";
	private static final String THUMBNAIL_PATH = "/storage/sdcard1/thumbnail/";
		
	private static final int 	THUMBNAIL_WIDTH	= 160 * 3 / 4;
	private static final int 	THUMBNAIL_HEIGHT=  90 * 3 / 4;
	
	public static Context mContext;
	
	ArrayList<MediaInfo> mMediaInfo;
	Cursor 	mVidCursor, mAudCursor;
	int		mItemPos, mItemNum;
	
	String 	mNetworkStreamName;
	boolean mNetworkStream;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// Overriding Function
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(DBG_TAG, "onCreate()++");
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_main );
		
	    // TODO Auto-generated method stub
		mContext = this;
		mItemNum = 0;
		
		String[] vidProj = {
			MediaStore.Video.Media._ID,
			MediaStore.Video.Media.DATA,
			MediaStore.Video.Media.DISPLAY_NAME,
			MediaStore.Video.Media.SIZE
		};

		String[] audProj = {
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.DISPLAY_NAME,
			MediaStore.Audio.Media.SIZE
		};
		
		String mediaId, mediaData, mediaName, mediaSize;
		int mediaIdCol, mediaDataCol, mediaNameCol, mediaSizeCol;
		
		mMediaInfo = new ArrayList<MediaInfo>();
		
		if( mVidCursor == null ) {
			mVidCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, vidProj, null, null, null);
			if( mVidCursor != null && mVidCursor.moveToFirst() != false ) {
				mediaIdCol		= mVidCursor.getColumnIndex(MediaStore.Video.Media._ID);
				mediaDataCol	= mVidCursor.getColumnIndex(MediaStore.Video.Media.DATA);  
				mediaNameCol	= mVidCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);  
				mediaSizeCol	= mVidCursor.getColumnIndex(MediaStore.Video.Media.SIZE);

				do {
					mediaId		= mVidCursor.getString(mediaIdCol);
					mediaData	= mVidCursor.getString(mediaDataCol);
					mediaName	= mVidCursor.getString(mediaNameCol); 
					mediaSize	= mVidCursor.getString(mediaSizeCol);

					if( mediaId != null ){
						mItemNum++;
						MediaInfo info = new MediaInfo(mediaId, mediaData, mediaName, mediaSize, MEDIA_TAG.VIDEO);
						mMediaInfo.add( info );
						//Log.v(DBG_TAG, "vidId(" + mediaId + "), vidData(" + mediaData + "), vidName(" + mediaName + "), vidSize(" + mediaSize + ")");
					}
				} while( mVidCursor.moveToNext() );
			}
		}
		
		if( mAudCursor == null ) {
			mAudCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audProj, null, null, null);
			if( mAudCursor != null && mAudCursor.moveToFirst() != false ) {
				mediaIdCol		= mAudCursor.getColumnIndex(MediaStore.Audio.Media._ID);
				mediaDataCol	= mAudCursor.getColumnIndex(MediaStore.Audio.Media.DATA);  
				mediaNameCol	= mAudCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);  
				mediaSizeCol	= mAudCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);

				do {
					mediaId		= mAudCursor.getString(mediaIdCol);
					mediaData	= mAudCursor.getString(mediaDataCol);
					mediaName	= mAudCursor.getString(mediaNameCol); 
					mediaSize	= mAudCursor.getString(mediaSizeCol);

					if( mediaId != null ){
						mItemNum++;
						MediaInfo info = new MediaInfo(mediaId, mediaData, mediaName, mediaSize, MEDIA_TAG.AUDIO);
						mMediaInfo.add( info );
						//Log.v(DBG_TAG, "audId(" + mediaId + "), audData(" + mediaData + "), audName(" + mediaName + "), audSize(" + mediaSize + ")");
					}
				} while( mAudCursor.moveToNext() );
			}
		}
		
		MediaInfoAdapter adapter = new MediaInfoAdapter( this, R.layout.listview_row, mMediaInfo );
		setListAdapter(adapter);
		
		Log.v(DBG_TAG, "onCreate()--");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if( id == R.id.main_networkstream ) {
			AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
			final EditText editText = new EditText(this);
			
			alertDlg.setTitle( "Network Stream" );
			alertDlg.setView(editText);
			alertDlg.setMessage(
				"Input URL:\n  Example: http://www.example.com/sample.mkv")
				.setCancelable(false)
				.setPositiveButton("Open", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Open Action
						mNetworkStreamName = editText.getText().toString().trim();
						if( mNetworkStreamName.isEmpty() != true ) {
							mNetworkStream = true;
							
							Toast.makeText(MainActivity.this, mNetworkStreamName, Toast.LENGTH_SHORT).show();
							
							Intent intent = new Intent( MainActivity.this, PlayerActivity.class);
							startActivity(intent);
							finish();
						}
						else {
							Toast.makeText(MainActivity.this, "Invalid Netwrok URL.", Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					}
				})
				.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
			alertDlg.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		//Log.v(DBG_TAG, "OnItemClickListener()++");
		
		mItemPos = position;
		mNetworkStream = false;

		Toast.makeText(MainActivity.this, mMediaInfo.get(position).GetData(), Toast.LENGTH_SHORT).show();
		
		Intent intent = new Intent( MainActivity.this, PlayerActivity.class);
		startActivity(intent);
		finish();
		//Log.v(DBG_TAG, "OnItemClickListener()--");
		
	}
	
	@Override
	protected void onDestroy() {
		//Log.v(DBG_TAG, "onDestroy()++");
		
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if( mVidCursor != null ) {
			mVidCursor.close();
			mVidCursor = null;
		}
		
		if( mAudCursor != null ) {
			mAudCursor.close();
			mAudCursor = null;
		}
		
		//Log.v(DBG_TAG, "onDestroy()--");
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Interface Function
	//
	public String GetFileName() {
		if( mNetworkStream )
			return mNetworkStreamName;
		else
			return mMediaInfo.get( mItemPos ).GetData();
	}

	public String GetNextFileName() {
		mItemPos++;
		if( mItemPos >= mItemNum ) mItemPos = 0;
		
		return mMediaInfo.get( mItemPos ).GetData();
	}
	
	public String GetPrevFileName() {
		mItemPos--;
		if( mItemPos < 0 ) mItemPos = mItemNum - 1;

		return mMediaInfo.get( mItemPos ).GetData();
	}
	
	public boolean IsNetworkStream()
	{
		return mNetworkStream;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Adapter of Media Information
	//
	private class MediaInfoAdapter extends ArrayAdapter<MediaInfo> {
		private ArrayList<MediaInfo> mItems;
		
		public MediaInfoAdapter(Context context, int textViewResourceId, ArrayList<MediaInfo> items) {
			super(context, textViewResourceId, items);
			this.mItems = items;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if( convertView == null ) {
				LayoutInflater li = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				
				// View inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot)
				convertView = li.inflate( R.layout.listview_row, parent, false );
			}
			
			MediaInfo mediaInfo = mItems.get(position);

			if( mediaInfo != null )
			{
				ImageView imageView1 = (ImageView)convertView.findViewById(R.id.imageView1);
				TextView textView1 = (TextView)convertView.findViewById(R.id.textView1);
				
				if( imageView1 != null ) {
if( SUPPORT_THUMBNAIL )
{
					if( Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB ) {
						new ThumbnailTask(imageView1, mediaInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					} else {
						new ThumbnailTask(imageView1, mediaInfo).execute();
					}
}
else {
	RelativeLayout.LayoutParams param  = new RelativeLayout.LayoutParams(0, 40);
	imageView1.setLayoutParams( param);
	Drawable alpha = imageView1.getDrawable();
	alpha.setAlpha(0);
}
				}

				if( textView1 != null ) {
					textView1.setText(mediaInfo.GetName());
				}
			}

			return convertView;
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
		private ImageView mImgView;
		private MediaInfo mMediaInfo;
		
		public ThumbnailTask(ImageView imgView, MediaInfo mediaInfo) {
			mImgView 	= imgView;
			mMediaInfo	= mediaInfo;
		}
		
		@Override
		protected Bitmap doInBackground(Void... params) {
			// TODO Auto-generated method stub
			Bitmap srcBitmap = null;
			Bitmap dstBitmap = null;

			// a. video case
			if( mMediaInfo.GetTag() == MEDIA_TAG.VIDEO ) {
				//Log.v(DBG_TAG, "VIDEO: " + mMediaInfo.GetName());

				String uriPath = THUMBNAIL_PATH + mMediaInfo.GetName() + ".jpg";
				File file = new File( uriPath );
				if( file.exists() == true ) {
					srcBitmap = BitmapFactory.decodeFile( uriPath );
					dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
				}
				else {
					// Thumbnail creation : thumbnail_init
					// Temperary code.
					Resources res = mContext.getResources();
					srcBitmap = BitmapFactory.decodeResource(res, R.drawable.thumbnail_init);
					dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
				}
			}
			// b. audio case
			else {
				//Log.v(DBG_TAG, "AUDIO: " + mMediaInfo.GetName());
				Resources res = mContext.getResources();
				srcBitmap = BitmapFactory.decodeResource(res, R.drawable.audio_only);
				dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
			}

			if( srcBitmap != null ) srcBitmap.recycle();
			return dstBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			if( result != null ) {
				mImgView.setImageBitmap( result );
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Data of Media Information
	//
	enum MEDIA_TAG 			{ VIDEO, AUDIO }

	class MediaInfo {
		private String 		mId;
		private String 		mData;
		private String		mName;
		private String		mSize;
		private MEDIA_TAG	mTag;
		
		public MediaInfo( String id, String data, String name, String size, MEDIA_TAG tag ) {
			this.mId	= id;
			this.mData	= data;
			this.mName	= name;
			this.mSize	= size;
			this.mTag	= tag;
		}
		public String GetId() {
			return mId;
		}
		public String GetData() {
			return mData;
		}
		public String GetName() {
			return mName;
		}
		public String GetSize() {
			return mSize;
		}
		public MEDIA_TAG GetTag() {
			return mTag;
		}
	}	
}


