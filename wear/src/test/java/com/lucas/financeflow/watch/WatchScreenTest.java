package com.lucas.financeflow.watch;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.lucas.financeflow.wearlink.WearProtocol;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30,qualifiers="w227dp-h227dp-round-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WatchScreenTest {
    @Test public void quickExpenseFormatsMoneyAndSavesExactlyOnce() throws Exception {
        android.app.Application app=RuntimeEnvironment.getApplication(); app.getSharedPreferences("watch",0).edit().clear().commit();
        new WatchStore(app).summary(new JSONObject().put("version",1).put("month",WearProtocol.month()).put("balanceCents",123456).put("updatedAt",System.currentTimeMillis()).put("accounts",new JSONArray().put("Carteira")).toString());
        try(org.robolectric.android.controller.ActivityController<WatchActivity> controller=Robolectric.buildActivity(WatchActivity.class).create().start().visible()) {
            WatchActivity activity=controller.get(); View root=activity.findViewById(android.R.id.content);
            render(root,"home"); click(root,"− Saída"); EditText amount=findAmount(root); assertNotNull(amount); amount.setText("12345"); assertEquals("123,45",amount.getText().toString()); render(root,"form");
            click(root,"Revisar"); render(root,"review"); click(root,"Salvar");
            JSONArray queue=new WatchStore(app).pending(); assertEquals(1,queue.length()); assertEquals(12345,queue.getJSONObject(0).getLong("cents"));
            assertTrue(queue.getJSONObject(0).getLong("notBefore")>System.currentTimeMillis());
            click(root,"Desfazer"); assertEquals(0,new WatchStore(app).pending().length());
        }
    }
    @Test public void watchOffersEveryPhoneCategory() {
        android.app.Application app=RuntimeEnvironment.getApplication();
        try {new WatchStore(app).summary(new JSONObject().put("version",1).put("accounts",new JSONArray().put("Carteira")).toString());} catch(Exception e) {throw new AssertionError(e);}
        try(var controller=Robolectric.buildActivity(WatchActivity.class).create().start().visible()) {
            View root=controller.get().findViewById(android.R.id.content); click(root,"− Saída"); click(root,"Outros");
            for(String category:com.lucas.financeflow.wearlink.CategoryCatalog.all()) assertNotNull(category,find(root,category));
            click(root,"Cartão"); assertNotNull(find(root,"Cartão"));
        }
    }
    private void click(View view,String text) { View target=find(view,text); assertNotNull(text,target); target.performClick(); }
    private View find(View view,String text) { if(view instanceof Button && text.contentEquals(((Button)view).getText())) return view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) { View found=find(((ViewGroup)view).getChildAt(i),text); if(found!=null) return found; } return null; }
    private EditText findAmount(View view) { if(view instanceof EditText) return (EditText)view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) { EditText found=findAmount(((ViewGroup)view).getChildAt(i)); if(found!=null) return found; } return null; }
    private void render(View root,String name) throws Exception {
        int size=454; root.measure(View.MeasureSpec.makeMeasureSpec(size,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size,View.MeasureSpec.EXACTLY)); root.layout(0,0,size,size);
        root.getViewTreeObserver().dispatchOnPreDraw();
        Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(bitmap); canvas.drawColor(0xff101b16); root.draw(canvas);
        java.io.File file=new java.io.File("build/previews/watch-"+name+".png"); file.getParentFile().mkdirs(); try(java.io.FileOutputStream stream=new java.io.FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,stream); }
    }
}
