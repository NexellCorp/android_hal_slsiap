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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.text.Collator;

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

import android.graphics.drawable.Drawable;
import android.widget.RelativeLayout;

public class MainActivity extends ListActivity {
	private static final boolean SUPPORT_THUMBNAIL = false;
	
	private static final String DBG_TAG = "NxPlayerBasedFilter.MainActivity";
	private static final String THUMBNAIL_PATH = "/storage/sdcard1/thumbnail/";
		
	private static final int 	THUMBNAIL_WIDTH	= 160 * 3 / 4;
	private static final int 	THUMBNAIL_HEIGHT=  90 * 3 / 4;

	private static final String[] VIDEO_EXTENSION = { 
		".avi",		".wmv",		".wmp",		".wm",		".asf",
		".mpg",		".mpeg",	".mpe",		".m1v",		".m2v",
		".mpv2",	".mp2v",	".dat",		".ts",		".tp",
		".tpr",		".trp", 	".vob", 	".ifo", 	".ogm",
		".ogv",		".mp4",		".m4v",		".m4p",		".m4b",
		".3gp",		".3gpp",	".3g2",		".3gp2",	".mkv",
		".rm",		".ram",		".rmvb",	".rpm",		".flv",
		".swf",		".mov",		".qt",		".amr",		".nsv",
		".dpg",		".m2ts",	".m2t",		".mts",		".dvr-ms",
		".k3g",		".skm",		".evo",		".nsr",		".amv",
		".divx",	".webm",	".wtv",		".f4v",	
	};
	
	private static final String[] AUDIO_EXTENSION = {
		"wav",		"wma",		"mpa",		"mp2",		"m1a",
		"m2a",		"mp3",		"ogg",		"m4a",		"aac",
		"mka",		"ra",		"flac",		"ape",		"mpc",
		"mod",		"ac3",		"eac3",		"dts",		"dtshd",
		"wv",		"tak", 
	};
	
	public static Context mContext;
	
	ArrayList<MediaInfo> mMediaInfo;
	static int	mItemPos;
	
	String 	mNetworkStreamName;
	boolean mNetworkStream;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// Overriding Function
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.v(DBG_TAG, "onCreate()++");
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_main );
		
	    // TODO Auto-generated method stub
		mContext = this;
		mMediaInfo = new ArrayList<MediaInfo>();
		
		UpdateFileList( "/storage/sdcard0" );
		UpdateFileList( "/storage/sdcard1" );
		
		Collections.sort( mMediaInfo , mComparator );
		
		MediaInfoAdapter adapter = new MediaInfoAdapter( this, R.layout.listview_row, mMediaInfo );
		setListAdapter( adapter );
		setSelection( mItemPos );
		
		//Log.v(DBG_TAG, "onCreate()--");
	}

	private final static Comparator<MediaInfo> mComparator = new Comparator<MediaInfo>() {
		private final Collator collator = Collator.getInstance();
		
		@Override
		public int compare(MediaInfo object1, MediaInfo object2) {
			// Sorting File Name 
			//return collator.compare(object1.GetFileName(), object2.GetFileName());
			
			// Sorting Full path
			return collator.compare(object1.GetFilePath(), object2.GetFilePath());
		}
	};
	
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

		Toast.makeText(MainActivity.this, mMediaInfo.get(position).GetFilePath(), Toast.LENGTH_SHORT).show();
		
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
		
		//Log.v(DBG_TAG, "onDestroy()--");
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	FileList
	//
	private void UpdateFileList( String rootPath )
	{
		File rootFile = new File( rootPath );
		
		if( !rootFile.exists() ) {
			Log.w(DBG_TAG, "Invalid Directory Path. (" + rootPath + ")" );
			return;
		}
		
		if( rootFile.isDirectory() ) {
			String[] rootList = rootFile.list();
			for( int i = 0; i < rootList.length; i++ )
			{
				String subPath = rootPath + "/" + rootList[i];
				File subFile = new File( subPath );
				String[] subList = subFile.list();
				
				if( subFile.isDirectory() )
				{
					for( int j = 0; j < subList.length; j++ )
					{
						UpdateFileList( subPath + "/" + subList[j] );
					}
				}
				else {
					if( isVideo(subPath) ) {
						//Log.v(DBG_TAG, "[VIDEO] " + subPath);
						MediaInfo info = new MediaInfo( subPath, subFile.getName(), null, MEDIA_TAG.VIDEO );
						mMediaInfo.add(info);
					}
					else if( isAudio(subPath) ) {
						//Log.v(DBG_TAG, "[AUDIO] " + subPath);
						MediaInfo info = new MediaInfo( subPath, subFile.getName(), null, MEDIA_TAG.AUDIO );
						mMediaInfo.add(info);
					}
				}
			}
		}
		else {
			if( isVideo(rootPath) ) {
				//Log.v(DBG_TAG, "[VIDEO] " + rootPath);
				MediaInfo info = new MediaInfo( rootPath, rootFile.getName(), null, MEDIA_TAG.VIDEO );
				mMediaInfo.add(info);
			}
			else if( isAudio(rootPath) ) {
				//Log.v(DBG_TAG, "[AUDIO] " + rootPath);
				MediaInfo info = new MediaInfo( rootPath, rootFile.getName(), null, MEDIA_TAG.AUDIO );
				mMediaInfo.add(info);
			}
		}
	}
	
	public boolean isVideo( String fileName )
	{
		String strTemp = fileName.toLowerCase();
		
		for( int i = 0; i < VIDEO_EXTENSION.length; i++ ) {
			if( strTemp.endsWith(VIDEO_EXTENSION[i]) ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isAudio( String fileName )
	{
		String strTemp = fileName.toLowerCase();
		
		for( int i = 0; i < AUDIO_EXTENSION.length; i++ ) {
			if( strTemp.endsWith(AUDIO_EXTENSION[i]) ) {
				return true;
			}
		}
		return false;	
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Interface Function
	//
	public String GetFileName() {
		if( mNetworkStream )
			return mNetworkStreamName;
		else
			return mMediaInfo.get( mItemPos ).GetFilePath();
	}

	public String GetNextFileName() {
		mItemPos++;
		if( mItemPos >= mMediaInfo.size() ) mItemPos = 0;
		
		return mMediaInfo.get( mItemPos ).GetFilePath();
	}
	
	public String GetPrevFileName() {
		mItemPos--;
		if( mItemPos < 0 ) mItemPos = mMediaInfo.size() - 1;

		return mMediaInfo.get( mItemPos ).GetFilePath();
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
					textView1.setText(mediaInfo.GetFileName());
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
			if( mMediaInfo.GetFileTag() == MEDIA_TAG.VIDEO ) {
				//Log.v(DBG_TAG, "VIDEO: " + mMediaInfo.GetName());

				String uriPath = THUMBNAIL_PATH + mMediaInfo.GetFileName() + ".jpg";
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
		private String 		mFilePath;
		private String		mFileName;
		private String		mFileSize;
		private MEDIA_TAG	mFileTag;
		
		public MediaInfo( String path, String name, String size, MEDIA_TAG tag ) {
			this.mFilePath	= path;
			this.mFileName	= name;
			this.mFileSize	= size;
			this.mFileTag	= tag;
		}
		public String GetFilePath() {
			return mFilePath;
		}
		public String GetFileName() {
			return mFileName;
		}
		public String GetFileSize() {
			return mFileSize;
		}
		public MEDIA_TAG GetFileTag() {
			return mFileTag;
		}
	}	
}
