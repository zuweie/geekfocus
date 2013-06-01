package oz.geekfocus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSItem;
import org.mcsoxford.rss.RSSReader;

import oz.geekfocus.me.Rssbean;
import oz.geekfocus.me.util;


public class NewsListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geekfocus);
		
		newslistview = (PullToRefreshListView)findViewById(R.id.newslistview);
		
		newslistview.setOnRefreshListener(new OnRefreshListener<ListView>(){
			
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				// TODO Auto-generated method stub
				String[] urls = {NEWSURL};
				new getStartUpNewslist().execute(urls);
			}
			
		});
		
		newslistview.setOnScrollListener(new OnScrollListener(){

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				
				if ((firstVisibleItem + visibleItemCount >= totalItemCount)
					&& (rss != null && rss.size() > totalItemCount)){
					//BaseAdapter newslistadapter = (BaseAdapter) view.getAdapter();
					getDatatoshow(false);
					//newslistadapter.notifyDataSetChanged();
					geekfocusmsghandler.sendEmptyMessage(MSG_UPDATELISTVIEW);
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}});
		
		newslistview.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Rssbean bean = (Rssbean)arg0.getAdapter().getItem(arg2);
				try {
					
					String postsurl = POSTSAPI+util.md5(bean.url);
					if (it == null)
						it = new Intent(NewsListActivity.this, PostActivity.class);
					
					it.putExtra("api", postsurl);
					
					startActivity(it);
					
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}});
		
		newslistadapter = new ArrayAdapter<Rssbean>(this, android.R.layout.simple_list_item_1, beanshow);
		
		newslistview.setAdapter(newslistadapter);
		
		geekfocusmsghandler = new GeekfocusMsgHandler();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.news_list, menu);
		return true;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dlg = null;
		switch(id){
		case DLG_LOADING:
			ProgressDialog pdlg = new ProgressDialog(NewsListActivity.this);
			pdlg.setProgress(ProgressDialog.STYLE_SPINNER);
			pdlg.setMessage(this.getResources().getString(R.string.dlgmsg_load));
			dlg = pdlg;
			break;
		default:
			super.onCreateDialog(id);
		}
		return dlg;
	}
	

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id){
		case DLG_LOADING:
			ProgressDialog pdlg = (ProgressDialog)dialog;
			pdlg.setProgress(0);
			break;
		}
	}
	

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		//geekfocusmsghandler.sendEmptyMessage(MSG_INITLOADRSS);
		/*
		 *  1 load the rss from local file
		 *  2 if nill or fail load it from startupnews
		 */
		
		if (loadRssfromlocal()){
			getDatatoshow(true);
			geekfocusmsghandler.sendEmptyMessage(MSG_UPDATELISTVIEW);
		}else{
			new loadRss();
		}
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		saveRss();
		super.onDestroy();
	}

	private boolean loadRssfromlocal(){
		FileInputStream fis = null;
		boolean ok = false;
		try {
			fis = openFileInput(RSSFILE);
			int c;
			StringBuffer json = new StringBuffer();
			while((c = fis.read()) != -1){
				json.append((char)c);
			}
			JSONObject Jobj = new JSONObject(json.toString());
			JSONArray jrsses = Jobj.getJSONArray(RSS);
			
			rss.clear();
			
			for (int i=0; i<jrsses.length(); ++i){
				JSONObject jrss = (JSONObject) jrsses.get(i);
				Rssbean bean = new Rssbean();
				
				bean.title = jrss.getString(TITLE);
				bean.url   = jrss.getString(URL);
				rss.add(bean);
			}
			
			//fis.close();
			ok = true;
		} catch (FileNotFoundException e) {
			ok = false;
		} catch (IOException e) {
			ok = false;
		} catch (JSONException e) {
			ok = false;
		}finally{
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
			}
		}
		return ok;
	}
	
	private boolean saveRss() {
		if (rss != null && rss.size() != 0){
			FileOutputStream fos = null;
			boolean ok = false;
			try {
				fos = openFileOutput(RSSFILE, Context.MODE_PRIVATE);
				JSONObject Jobj = new JSONObject();
				//Date date = new Date();
				//Jobj.put(UPDATED_AT, date.toString());
				JSONArray jrsses = new JSONArray();
				
				for (int i=0; i<rss.size(); ++i){
					JSONObject jrss = new JSONObject();
					Rssbean bean = rss.get(i);
					jrss.put(TITLE, bean.title);
					jrss.put(URL, bean.url);
					jrsses.put(jrss);
				}
				
				Jobj.put(RSS, jrsses);
				fos.write(Jobj.toString().getBytes("utf-8"));
				//fos.close();
			} catch (FileNotFoundException e) {
				ok = false;
			} catch (JSONException e) {
				ok = false;
			} catch (IOException e) {
				ok = false;
			}finally{
				if (fos != null){
					try {
						fos.close();
					} catch (IOException e) {
						
					}
				}
			}
			return ok;
		}
		return false;
	}
	
	private void loadRssfromStartupnews() {
		try {
			//RssParser Rss = new RssParser();
			//RssFeed feed = Rss.load(NEWSURL);
			//rss = feed.getItems();
			
			RSSReader reader = new RSSReader();
			RSSFeed feed = reader.load(NEWSURL);
			List<RSSItem> items = feed.getItems();
			rss.clear();
			for (int i=0; i<items.size(); ++i){
				RSSItem item = items.get(i);
				Rssbean bean = new Rssbean();
				bean.title = item.getTitle();
				bean.url   = item.getLink().toString();
				rss.add(bean);
			}
		} catch (Exception e) {
			Log.e(ERR_RSS, e.getMessage());
		}
	}
	
	private void getDatatoshow(boolean hadreload){
		if (hadreload){
			int count = beanshow.size();
			beanshow.clear();
			count = (rss.size() > NEWSHOW)? NEWSHOW : rss.size();
			for (int i=0; i<count; ++i){
				beanshow.add(rss.get(i));
			}
		}else{
			// add the old one behide;
			//beanshow.clear();
			int count = beanshow.size() + NEWSHOW;
			if (count > rss.size())
				count = rss.size();
			for (int i=0; i<count; ++i){
				beanshow.add(rss.get(i));
			}
		}
	}
	
	private class getStartUpNewslist extends AsyncTask<String, Void, String[]> {
		
	    @Override
	    protected void onPostExecute(String[] result) {
	        // Call onRefreshComplete when the list has been refreshed.
	    	geekfocusmsghandler.sendEmptyMessage(MSG_UPDATELISTVIEW);
	    	newslistview.onRefreshComplete();
	        super.onPostExecute(result);
	    }

		@Override
		protected String[] doInBackground(String... params) {
			loadRssfromStartupnews();
			getDatatoshow(true);
			return null;
		}
	}
	
	class GeekfocusMsgHandler extends Handler {

		@SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_UPDATELISTVIEW:
				newslistadapter.notifyDataSetChanged();
				break;
			case MSG_OPENPROGRESSDLG:
				showDialog(DLG_LOADING);
				break;
			case MSG_CLOSEPROGRESSDLG:
				dismissDialog(DLG_LOADING);
				break;
			default:
				super.handleMessage(msg);
			}
		}
		
	}
	
	class loadRss extends Thread {
		public loadRss (){
			this.start();
		}

		@Override
		public void run() {
			/* 
			 *  1 open the progress dialog
			 *  2 load the new list from start up news;
			 *  3 feed the list view 
			 *  4 close the progress dialog
			 */
			geekfocusmsghandler.sendEmptyMessage(MSG_OPENPROGRESSDLG);
			loadRssfromStartupnews();
			getDatatoshow(true);
			geekfocusmsghandler.sendEmptyMessage(MSG_CLOSEPROGRESSDLG);
			geekfocusmsghandler.sendEmptyMessage(MSG_UPDATELISTVIEW);
			
		}
	}
	
	
	private PullToRefreshListView newslistview;
	private GeekfocusMsgHandler geekfocusmsghandler;
	private List<Rssbean> rss = new ArrayList<Rssbean>();
	private List<Rssbean> beanshow = new LinkedList<Rssbean>();
	
	private final static int NEWSHOW  = 20;
	private ArrayAdapter<Rssbean> newslistadapter = null;
	//Newslistadapter newsadapter = null;
	
	private final static String NEWSURL = "http://news.dbanotes.net/rss";
	private final static String POSTSAPI = "121.199.25.185/posts?code=";
	private final static String RSSFILE = "rss.txt";
	
	private final static String RSS        = "rss";
	// rss 
	private final static String TITLE      = "title";
	private final static String URL 	   = "url";
	//
	private final static String ERR_RSS    = "RSS";
	
	private final static int MSG_UPDATELISTVIEW = 1;
	private final static int MSG_OPENPROGRESSDLG = 2;
	private final static int MSG_CLOSEPROGRESSDLG = 3;
	
	private final static int DLG_LOADING = 1;
	Intent it = null;
}
