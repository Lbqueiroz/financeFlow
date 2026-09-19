package com.lucas.financeflow;

import android.content.Intent;
import android.view.*;
import android.widget.*;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w411dp-h891dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlanningScreenTest {
    private AppDatabase db;
    @Before public void setup() throws Exception {
        db=androidx.room.Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),AppDatabase.class).allowMainThreadQueries().setQueryExecutor(Runnable::run).setTransactionExecutor(Runnable::run).build();
        java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,db);
        db.lancamentoDao().cadastrar(new Cadastro("CONTA","Banco")); db.lancamentoDao().cadastrar(new Cadastro("CONTA","Carteira"));
    }
    @After public void close() throws Exception {db.close(); java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,null);}
    @Test public void transferFormMovesMoneyAndScreensRender() throws Exception {
        Intent intent=new Intent(RuntimeEnvironment.getApplication(),PlanningActivity.class).putExtra("mode","TRANSFER");
        try(var controller=Robolectric.buildActivity(PlanningActivity.class,intent).setup()) {
            PlanningActivity activity=controller.get(); ShadowLooper.idleMainLooper(); button(activity.findViewById(android.R.id.content),"+ Transferir").performClick(); ShadowLooper.idleMainLooper();
            androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            View root=dialog.findViewById(android.R.id.content);
            ((EditText)find(root,"Valor (R$)")).setText("15000");
            ((Spinner)find(root,"Destino")).setSelection(1); dialog.getButton(-1).performClick();
            waitFor(() -> !dialog.isShowing()); assertEquals(1,db.planDao().snapshot().size()); assertEquals(15000,db.planDao().snapshot().get(0).cents); capture(activity,"transferencias");
        }
        PlanItem asset=new PlanItem(); asset.kind="ASSET"; asset.name="Reserva de emergência"; asset.account="Minha corretora"; asset.date=FinanceUtils.hoje(); db.planDao().insert(asset);
        PlanItem deposit=new PlanItem(); deposit.kind="APPLY"; deposit.account="Banco"; deposit.target=asset.id; deposit.cents=150000; deposit.date=FinanceUtils.hoje(); deposit.created=asset.created+1; db.planDao().insert(deposit);
        PlanItem budget=new PlanItem(); budget.kind="BUDGET"; budget.category="Alimentação"; budget.cents=60000; budget.date=FinanceUtils.hoje(); db.planDao().insert(budget);
        PlanItem rule=new PlanItem(); rule.kind="RULE"; rule.name="Internet"; rule.account="Banco"; rule.category="Conta fixa"; rule.day=15; rule.date=FinanceUtils.hoje(); rule.cents=9990; db.planDao().insert(rule);
        for(String mode:new String[]{"ASSET","BUDGET","RULE"}) try(var controller=Robolectric.buildActivity(PlanningActivity.class,new Intent(RuntimeEnvironment.getApplication(),PlanningActivity.class).putExtra("mode",mode)).setup()) {ShadowLooper.idleMainLooper(); capture(controller.get(),mode.toLowerCase(java.util.Locale.ROOT));}
    }
    @Test public void savingAndUndoingNewEntryLeavesNoTransaction() throws Exception {
        try(var controller=Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            var activity=controller.get(); ShadowLooper.idleMainLooper(); ((EditText)activity.findViewById(R.id.form_descricao)).setText("Teste"); ((EditText)activity.findViewById(R.id.form_valor)).setText("100"); ((Spinner)activity.findViewById(R.id.form_conta)).setSelection(1);
            button(activity.findViewById(android.R.id.content),"Salvar lançamento").performClick(); waitFor(() -> button(activity.findViewById(android.R.id.content),"Desfazer")!=null);
            assertEquals(1,db.lancamentoDao().snapshot().size()); button(activity.findViewById(android.R.id.content),"Desfazer").performClick(); waitFor(activity::isFinishing); assertTrue(db.lancamentoDao().snapshot().isEmpty());
        }
    }
    private void waitFor(java.util.function.BooleanSupplier condition) throws Exception {long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10); while(!condition.getAsBoolean() && System.nanoTime()<end) {ShadowLooper.idleMainLooper(); Thread.sleep(10);} assertTrue(condition.getAsBoolean());}
    private View find(View view,String label) {if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription())) return view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {View found=find(((ViewGroup)view).getChildAt(i),label); if(found!=null) return found;} return null;}
    private Button button(View view,String text) {if(view instanceof Button && text.contentEquals(((Button)view).getText())) return (Button)view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {Button found=button(((ViewGroup)view).getChildAt(i),text); if(found!=null) return found;} return null;}
    private void capture(android.app.Activity activity,String name) throws Exception {
        View root=activity.findViewById(android.R.id.content); root.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(843,View.MeasureSpec.EXACTLY)); root.layout(0,0,411,843); root.getViewTreeObserver().dispatchOnPreDraw();
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(411,843,android.graphics.Bitmap.Config.ARGB_8888); android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap); canvas.drawColor(0xfff4f7f5); root.draw(canvas);
        java.io.File file=new java.io.File("build/previews/"+name+".png"); file.getParentFile().mkdirs(); try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)) {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
    }
}
