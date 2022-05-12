package ru.eneanuch.megafacebookapp.listeners;

import ru.eneanuch.megafacebookapp.models.UserModel;

public interface ConversionListener {
    void onConversionClicked(UserModel user);
}
