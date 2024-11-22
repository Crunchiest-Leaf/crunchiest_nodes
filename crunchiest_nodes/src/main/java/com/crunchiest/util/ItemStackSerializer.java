package com.crunchiest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class ItemStackSerializer {
    public static String serialize(ItemStack itemStack) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeObject(itemStack);
        dataOutput.close();

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack deserialize(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        ItemStack itemStack = (ItemStack) dataInput.readObject();
        dataInput.close();

        return itemStack;
    }
}