package com.lucas.financeflow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationTest {
    @Test public void tabsOpenSeparateScreens() throws Exception {
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            BottomNavigationView nav = navigation(activity.findViewById(android.R.id.content));
            assertNotNull(nav); assertEquals(5, nav.getMenu().size());
            int[] ids = {R.id.nav_lancamentos, R.id.nav_contas, R.id.nav_categorias, R.id.nav_backup};
            Class<?>[] screens = {LancamentosActivity.class, ContasActivity.class, CategoriasActivity.class, BackupActivity.class};
            for (int i = 0; i < ids.length; i++) {
                nav.setSelectedItemId(ids[i]);
                assertEquals(screens[i].getName(), Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
            }
            capture(activity, "resumo");
        }
    }
    @Test public void formUsesSelectorsInLightThemeEvenAtNight() throws Exception {
        try (var controller = Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            AddLancamentoActivity activity = controller.get();
            assertTrue(activity.findViewById(R.id.form_categoria) instanceof Spinner);
            assertTrue(activity.findViewById(R.id.form_conta) instanceof Spinner);
            assertTrue(activity.findViewById(R.id.form_pessoa) instanceof Spinner);
            assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO, activity.getDelegate().getLocalNightMode());
            assertNull(navigation(activity.findViewById(android.R.id.content)));
            capture(activity, "cadastro");
        }
    }
    @Test public void accountAndCategoryTabsHaveTheirOwnContent() throws Exception {
        try (var accounts = Robolectric.buildActivity(ContasActivity.class).setup();
             var categories = Robolectric.buildActivity(CategoriasActivity.class).setup()) {
            assertEquals(R.id.nav_contas, navigation(accounts.get().findViewById(android.R.id.content)).getSelectedItemId());
            assertEquals(R.id.nav_categorias, navigation(categories.get().findViewById(android.R.id.content)).getSelectedItemId());
            capture(accounts.get(), "contas");
            capture(categories.get(), "categorias");
        }
    }
    private BottomNavigationView navigation(View view) {
        if (view instanceof BottomNavigationView) return (BottomNavigationView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            BottomNavigationView nav = navigation(((ViewGroup) view).getChildAt(i)); if (nav != null) return nav;
        }
        return null;
    }
    private void capture(android.app.Activity activity, String name) throws Exception {
        View root = activity.findViewById(android.R.id.content);
        root.measure(View.MeasureSpec.makeMeasureSpec(411, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(843, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 411, 843);
        Bitmap image = Bitmap.createBitmap(411, 843, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image); canvas.drawColor(android.graphics.Color.rgb(244,247,245)); root.draw(canvas);
        java.io.File folder = new java.io.File("build/previews"); folder.mkdirs();
        try (java.io.FileOutputStream file = new java.io.FileOutputStream(new java.io.File(folder, name + ".png"))) { image.compress(Bitmap.CompressFormat.PNG, 100, file); }
        image.recycle();
    }
}
