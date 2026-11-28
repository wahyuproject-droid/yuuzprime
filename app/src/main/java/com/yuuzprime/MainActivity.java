package com.yuuzprime;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    final ExecutorService pool = Executors.newFixedThreadPool(3);
    LinearLayout root, cards;
    TextView status;
    EditText search;
    Button more;
    String category="movie/now_playing", query="";
    int page=1, generation=0, total=1;
    boolean loading=false;
    String key(){return getPreferences(0).getString("tmdb","");}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b); home();}
    @Override public void onDestroy(){pool.shutdownNow();super.onDestroy();}
    void home(){
        generation++; loading=false;
        root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xff101010);root.setPadding(dp(16),dp(36),dp(16),dp(18));setContentView(root);
        TextView logo=text("YuuZPrime",30);logo.setTypeface(null,Typeface.BOLD);root.addView(logo);
        root.addView(text("Film dan serial • TMDB",14));
        LinearLayout bar=new LinearLayout(this);
        bar.addView(button("API",()->settings()),new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(button("Video lokal",()->pick()),new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(button("Credit",()->new AlertDialog.Builder(this).setTitle("YuuZPrime").setMessage("Modifikasi Android: YuuZGilakPrompt\nBasis katalog: TMDB-To-VOD-Playlist — gogetta69\nCC BY-NC 4.0\nUses TMDB API; not endorsed or certified by TMDB.").setPositiveButton("OK",null).show()),new LinearLayout.LayoutParams(0,-2,1));
        root.addView(bar);
        search=new EditText(this);search.setSingleLine();search.setHint("Cari film atau serial");root.addView(search);
        root.addView(button("Cari",()->{query=search.getText().toString().trim();reset();}));
        String[] labels={"Film terbaru","Film populer","Serial tayang","Serial populer","Serial top rated","Genre film","Genre serial"};
        String[] paths={"movie/now_playing","movie/popular","tv/on_the_air","tv/popular","tv/top_rated"};
        Spinner tabs=new Spinner(this);tabs.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(tabs);
        status=text("",14);root.addView(status);
        ScrollView scroll=new ScrollView(this);cards=new LinearLayout(this);cards.setOrientation(1);scroll.addView(cards);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        more=button("Muat lebih banyak",()->load());root.addView(more);
        tabs.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int i,long id){query="";if(i<5){category=paths[i];reset();}else genres(i==5?"movie":"tv");}});
        if(key().isEmpty())status.setText("Masukkan API key TMDB melalui menu API.");
    }
    void settings(){EditText input=new EditText(this);input.setSingleLine();input.setText(key());new AlertDialog.Builder(this).setTitle("TMDB v3 API key").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{getPreferences(0).edit().putString("tmdb",input.getText().toString().trim()).apply();reset();}).show();}
    String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    JSONObject request(String path,String api)throws Exception{
        URL u=new URL("https://api.themoviedb.org/3/"+path+(path.contains("?")?"&":"?")+"api_key="+enc(api)+"&language=id-ID&include_adult=false");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);
        try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return new JSONObject(out.toString("UTF-8"));}}finally{c.disconnect();}
    }
    void genres(String type){
        String api=key();if(api.isEmpty()){status.setText("API key belum diisi.");return;}
        int token=++generation;loading=false;status.setText("Memuat genre...");
        pool.execute(()->{try{JSONArray a=request("genre/"+type+"/list",api).getJSONArray("genres");String[] labels=new String[a.length()];int[] ids=new int[a.length()];for(int i=0;i<a.length();i++){labels[i]=a.getJSONObject(i).getString("name");ids[i]=a.getJSONObject(i).getInt("id");}runOnUiThread(()->{if(token!=generation||isFinishing())return;new AlertDialog.Builder(this).setTitle("Pilih genre").setItems(labels,(d,i)->{category="discover/"+type+"?with_genres="+ids[i];reset();}).setNegativeButton("Batal",(d,w)->status.setText("Pilih kategori lain.")).show();});}catch(Exception e){runOnUiThread(()->{if(token==generation)status.setText("Gagal memuat genre.");});}});
    }
    void reset(){generation++;page=1;total=1;loading=false;cards.removeAllViews();load();}
    void load(){
        String api=key();if(api.isEmpty()){status.setText("Masukkan API key TMDB.");return;}
        if(loading||page>total)return;loading=true;more.setEnabled(false);status.setText("Memuat...");
        int token=generation,p=page;String cat=category,q=query;
        pool.execute(()->{try{
            String path=q.isEmpty()?cat:"search/multi?query="+enc(q);
            JSONObject response=request(path+(path.contains("?")?"&":"?")+"page="+p,api);
            JSONArray results=response.getJSONArray("results");
            runOnUiThread(()->{if(token!=generation||isFinishing())return;
                total=Math.min(500,response.optInt("total_pages",1));page=p+1;loading=false;
                for(int i=0;i<results.length();i++){JSONObject item=results.optJSONObject(i);if(item==null||item.optBoolean("adult",false))continue;String type=item.optString("media_type");if(!type.isEmpty()&&!type.equals("movie")&&!type.equals("tv"))continue;addCard(item,token);}
                status.setText(cards.getChildCount()+" judul • halaman "+p);more.setEnabled(page<=total);
            });
        }catch(Exception e){runOnUiThread(()->{if(token==generation){loading=false;more.setEnabled(true);status.setText("Gagal memuat. Periksa API key dan koneksi.");}});}});
    }
    void addCard(JSONObject item,int token){
        String title=item.optString("title",item.optString("name","Tanpa judul"));
        LinearLayout row=new LinearLayout(this);row.setPadding(dp(8),dp(8),dp(8),dp(8));row.setBackgroundColor(0xff1c1c1c);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(poster,new LinearLayout.LayoutParams(dp(76),dp(112)));
        LinearLayout details=new LinearLayout(this);details.setOrientation(1);details.setPadding(dp(12),dp(8),0,0);details.addView(text(title,17));details.addView(text(item.optString("release_date",item.optString("first_air_date","")),13));details.addView(text("Rating "+item.optString("vote_average","—"),13));row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);cards.addView(row,lp);
        row.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(title).setMessage(item.optString("overview","Sinopsis belum tersedia.")).setPositiveButton("Tutup",null).show());
        String path=item.optString("poster_path","");if(path.matches("/[A-Za-z0-9_.-]+"))pool.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL("https://image.tmdb.org/t/p/w185"+path).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);try(InputStream in=c.getInputStream()){android.graphics.Bitmap bitmap=BitmapFactory.decodeStream(in);runOnUiThread(()->{if(token==generation&&!isFinishing())poster.setImageBitmap(bitmap);});}finally{c.disconnect();}}catch(Exception ignored){}});
    }
    void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,1);}catch(ActivityNotFoundException e){Toast.makeText(this,"Pemilih file tidak tersedia.",Toast.LENGTH_SHORT).show();}}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==1&&c==RESULT_OK&&data!=null&&data.getData()!=null){generation++;LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setBackgroundColor(Color.BLACK);layout.addView(button("Kembali",()->home()));VideoView player=new VideoView(this);layout.addView(player,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);MediaController controller=new MediaController(this);controller.setAnchorView(player);player.setMediaController(controller);player.setVideoURI(data.getData());player.setOnPreparedListener(m->player.start());player.setOnErrorListener((m,w,x)->{Toast.makeText(this,"Format video tidak didukung.",Toast.LENGTH_LONG).show();return true;});}}
}
