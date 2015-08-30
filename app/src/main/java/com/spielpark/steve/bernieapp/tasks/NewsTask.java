package com.spielpark.steve.bernieapp.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spielpark.steve.bernieapp.R;
import com.spielpark.steve.bernieapp.misc.ImgTxtAdapter;
import com.spielpark.steve.bernieapp.misc.Util;
import com.spielpark.steve.bernieapp.wrappers.Event;
import com.spielpark.steve.bernieapp.wrappers.NewsArticle;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Steve on 7/8/2015.
 */
public class NewsTask extends AsyncTask {
    private static ArrayList<NewsArticle> articles;
    private static ListView list;
    private static ProgressBar progressBar;
    private static Context ctx;
    private static TextView subHeader;
    private static TextView header;

    public NewsTask(Context ctx, ListView listView, ProgressBar progressBar, TextView subHeader, TextView header) {
        this.list = listView;
        this.ctx = ctx;
        this.progressBar = progressBar;
        this.subHeader = subHeader;
        this.header = header;
    }

    public static NewsArticle getArticle(int pos) {
        return articles.get(pos);
    }
    public static boolean hasData() { return articles != null && articles.size() != 0; }
    public static ArrayList<NewsArticle> getData() { return articles; }
    public static void clear() {
        articles = null;
        ctx = null;
        list = null;
        progressBar = null;
    }
    @Override
    protected Object doInBackground(Object[] params) {
        articles = new ArrayList<>();
        String[] feeds = new String[] {
                "https://berniesanders.com/feed/",
                "https://berniesanders.com/press-release/feed/"
        };
        for (String s : feeds) {
            readStream(s);
        }
        return null;
    }

    private void readStream(String feed) {
        BufferedReader xml = null;
        try {
            URL url = new URL(feed);
            xml = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (xml == null) {
            NewsArticle a = new NewsArticle();
            a.setTitle("Unable to Load News");
            a.setDesc("Check your internet connection?");
            articles.add(a);
            return;
        }
        XmlPullParser xmlReader = Xml.newPullParser();
        try {
            xmlReader.setInput(xml);
            readXml(xmlReader);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Collections.sort(articles);
        NewsArticle a;
        Log.d("#Articles", Integer.toString(articles.size()));
        boolean setSubheader = false;
        for (int i = 0; i < articles.size(); i++) {
            a = articles.get(i);
            a.setHtmlTitle(getHTMLForTitle(a));
            if (!(setSubheader)) {
                if (a.getUrl().contains("press-release")) {
                    subHeader.setText(Html.fromHtml(a.getDesc()));
                    String s = a.getTitle();
                    s = s.length() > 40 ? s.substring(0, 40) + "..." : s;
                    header.setText(s);
                    setSubheader = true;
                }
            }
        }
        ImgTxtAdapter adapter = new ImgTxtAdapter(ctx, R.layout.list_news_item, articles);
        list.setAdapter(adapter);
        new FetchNewsThumbs(list.getAdapter()).execute();
        list.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void formatDate(NewsArticle e) {
        SimpleDateFormat ft;
        ft = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        try {
            Date date = ft.parse(e.getPubdate());
            e.setPubdate(new SimpleDateFormat("MMMM d, yyyy").format(date));
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
    }

    private String getHTMLForTitle(NewsArticle e) {
        StringBuilder bld = new StringBuilder();
        String title = e.getTitle();
        if (title.length() > 40) {
            title = title.substring(0, 40);
            title = title.substring(0, Math.min(title.length(), title.lastIndexOf(' '))).concat("...");
        }
        bld.append("<big>").append(title).append("</big><br>");
        bld.append("<font color=\"#FF2222\">").append(e.getPubdate());
        return bld.toString();
    }

    private void readXml(XmlPullParser in) throws XmlPullParserException, IOException {

        int type = in.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG && in.getName().equals("item")) {
                readItem(in);
            }
            type = in.next();
        }
    }

    private void readItem(XmlPullParser in) throws XmlPullParserException, IOException {
        NewsArticle a = new NewsArticle();
        int type = in.next();
        while (!(type == XmlPullParser.END_TAG && in.getName().equals("item"))) {
            if (type == XmlPullParser.START_TAG) {
                String name = in.getName();
                if (name.equals("title")) {
                    a.setTitle(in.nextText());
                } else if (name.equals("link")) {
                    a.setUrl(in.nextText());
                } else if (name.equals("pubDate")) {
                    String t = (in.nextText());
                    String time = t.substring(t.indexOf(':') - 2, t.lastIndexOf(':') +2);
                    a.setTime(time);
                    a.setPubdate(t);
                } else if (name.equals("description")) {
                    a.setDesc(in.nextText());
                }
            }
            type = in.next();
        }
        formatDate(a);
        articles.add(a);
    }

    private static class FetchNewsThumbs extends AsyncTask {
        private static ListAdapter adapter;
        public FetchNewsThumbs(ListAdapter a) {
            this.adapter = a;
        }


        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (adapter != null ) {
                ((ImgTxtAdapter) NewsTask.list.getAdapter()).notifyDataSetChanged();
            }
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            ((ImgTxtAdapter) adapter).notifyDataSetChanged();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            for (NewsArticle a : articles) {
                a.setImg(Util.getOGImage(a.getUrl(), ctx, true));
                publishProgress();
            }
            return null;
        }
    }
}
