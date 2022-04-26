package net.blancworks.figura.utils;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {

    public static String noBadges4U(String string) {
        return string.replaceAll("([▲!❤☆✯★]|\\\\u(?i)(25B2|0021|2764|2606|272F|2605))", "\uFFFD");
    }

    public static List<Text> splitText(Text text, String regex) {
        //list to return
        ArrayList<Text> textList = new ArrayList<>();

        //current line variable
        MutableText currentText = Text.empty();

        //iterate over the text
        for (Text entry : text.getWithStyle(text.getStyle())) {
            //split text based on regex
            String entryString = entry.getString();
            String[] lines = entryString.split(regex);

            //iterate over the split text
            for (int i = 0; i < lines.length; i++) {
                //if it is not the first iteration, add to return list and reset the line variable
                if (i != 0) {
                    textList.add(currentText.shallowCopy());
                    currentText = Text.empty();
                }

                //append text with the line text
                currentText.append(Text.literal(lines[i]).setStyle(entry.getStyle()));
            }

            //if the text ends with the split pattern, add to return list and reset the line variable
            if (entryString.matches(".*" + regex + "$")) {
                textList.add(currentText.shallowCopy());
                currentText = Text.empty();
            }
        }
        //add the last text iteration then return
        textList.add(currentText);
        return textList;
    }

    public static Text removeClickableObjects(Text text) {
        //text to return
        MutableText finalText = Text.empty();

        //iterate over the text
        for (Text entry : text.getWithStyle(text.getStyle())) {
            //remove click events
            Text removed = Text.literal(entry.getString()).setStyle(entry.getStyle().withClickEvent(null));

            //append text to return
            finalText.append(removed);
        }

        //return text
        return finalText;
    }

    public static Text tryParseJson(String text) {
        //text to return
        Text finalText;

        try {
            //attempt to parse json
            finalText = Text.Serializer.fromJson(new StringReader(text));

            //if failed, throw a dummy exception
            if (finalText == null)
                throw new Exception("Error parsing JSON string");
        } catch (Exception ignored) {
            //on any exception, make the text as-is
            finalText = Text.literal(text);
        }

        //return text
        return finalText;
    }

    public static Text replaceInText(Text text, String regex, Text replacement) {
        //split the text based on the regex pattern
        List<Text> list = splitText(text, regex);

        //text to return
        MutableText finalText = Text.empty();

        //iterate over the split text
        for (int i = 0; i < list.size(); i++) {
            //append the split text on the return text
            finalText.append(list.get(i));

            //if it is not the last iteration, append the replacement text
            if (i < list.size() - 1)
                finalText.append(replacement);
        }

        //return the text
        return finalText;
    }

    //yoinked from ui helper
    public static void renderOutlineText(MatrixStack matrices, TextRenderer textRenderer, Text text, float x, float y, int color, int outline) {
        Text outlineText = Text.literal(text.getString().replaceAll("§.", "")).setStyle(text.getStyle().withColor(outline));
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                textRenderer.draw(matrices, outlineText, x + i, y + j, outline);
            }
        }

        matrices.push();
        matrices.translate(0f, 0f, 0.1f);
        textRenderer.draw(matrices, text, x, y, color);
        matrices.pop();
    }
}
