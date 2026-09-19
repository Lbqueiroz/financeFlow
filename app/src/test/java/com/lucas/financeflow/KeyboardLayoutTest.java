package com.lucas.financeflow;
import android.view.*;
import android.widget.*;
import androidx.core.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=30,qualifiers="w411dp-h843dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class KeyboardLayoutTest {
    @Test public void keyboardLeavesSearchAndResultsVisibleAndRestoresControls() throws Exception {
        try(var controller=Robolectric.buildActivity(LancamentosActivity.class).setup()) {
            var activity=controller.get(); View root=((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            EditText search=activity.findViewById(R.id.lista_busca); search.requestFocus(); search.setText("supermercado");
            apply(root,true); View list=activity.findViewById(R.id.lista_itens);
            assertTrue("Resultados sem espaço com teclado: "+list.getHeight(),list.getHeight()>=120);
            android.graphics.Rect visible=new android.graphics.Rect(); assertTrue(search.getGlobalVisibleRect(visible)); assertEquals(search.getHeight(),visible.height());
            assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(search.getCurrentTextColor(),android.graphics.Color.WHITE)>=4.5);
            capture(root,"busca-teclado");
            apply(root,false); assertEquals(View.VISIBLE,activity.findViewById(R.id.filtro_conta).getVisibility()); assertEquals("supermercado",search.getText().toString()); capture(root,"busca-filtros");
        }
    }
    private void apply(View root,boolean keyboard) {
        WindowInsetsCompat insets=new WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(),androidx.core.graphics.Insets.of(0,24,0,24))
            .setInsets(WindowInsetsCompat.Type.ime(),androidx.core.graphics.Insets.of(0,0,0,keyboard?360:0)).setVisible(WindowInsetsCompat.Type.ime(),keyboard).build();
        ViewCompat.dispatchApplyWindowInsets(root,insets);
        root.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(843,View.MeasureSpec.EXACTLY)); root.layout(0,0,411,843); root.getViewTreeObserver().dispatchOnPreDraw();
    }
    private void capture(View root,String name) throws Exception {
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(411,843,android.graphics.Bitmap.Config.ARGB_8888); android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap); canvas.drawColor(0xfff4f7f5); root.draw(canvas);
        java.io.File file=new java.io.File("build/previews/"+name+".png"); file.getParentFile().mkdirs(); try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)) {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
    }
}
