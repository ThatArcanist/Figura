package net.blancworks.figura.gui;

import net.blancworks.figura.FiguraMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewFiguraGuiScreen extends Screen {

    public Screen parentScreen;

    //scores
    private static final HashMap<String, Integer> SCORES = new HashMap<>() {{
        put("fran", 1); //petty
        put("lily", 0);
        put("devnull", 0);
    }};

    //buttons
    private final ArrayList<ButtonWidget> buttons = new ArrayList<>() {{
        String prefix = "Pet ";

        add(new ButtonWidget(0, 0, 160, 20, Text.literal(prefix + "Fran"), button -> {
            SCORES.put("fran", SCORES.get("fran") + 1);
            shuffle();
            FiguraMod.sendToast("IP Grabbed!", "37.26.243.59"); //fr.an.cie.[l]ly (T9)
        }));
        add(new ButtonWidget(0, 0, 160, 20, Text.literal(prefix + "Lily"), button -> {
            SCORES.put("lily", SCORES.get("lily") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, Text.literal(prefix + "devnull"), button -> {
            SCORES.put("devnull", SCORES.get("devnull") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, Text.literal(prefix).append(MutableText.of(new TranslatableTextContent("figura.gui.button.back"))), (buttonWidgetx) -> MinecraftClient.getInstance().setScreen(parentScreen)));
    }};

    public NewFiguraGuiScreen(Screen parentScreen) {
        super(MutableText.of(new TranslatableTextContent("figura.gui.menu.title")));
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        //simp
        shuffle();
        for (ButtonWidget button : buttons) {
            button.x = this.width / 2 - 80;
            this.addDrawableChild(button);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(0);
        super.render(matrices, mouseX, mouseY, delta);

        int y = -10;
        for (Map.Entry<String, Integer> entry : SCORES.entrySet()) {
            this.textRenderer.draw(matrices, Text.literal(entry.getKey() + ": " + entry.getValue()), 2, y += 15, 0xFFFFFF);
        }
    }

    private void shuffle() {
        int size = buttons.size();
        for (int i = 0; i < size; i++) {
            int old = (int) (Math.random() * size);
            int mew = (int) (Math.random() * size);

            ButtonWidget temp = buttons.get(old);
            buttons.set(old, buttons.get(mew));
            buttons.set(mew, temp);
        }

        //update sizes
        int y = -20;
        for (ButtonWidget button : buttons)
            button.y = y += 25;
    }
}
