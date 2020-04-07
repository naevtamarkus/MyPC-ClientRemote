package mypc.clientremote;

/*
    This is a Companion App that allows users to control their personal computer
    from their Android TV when they are using the MyPC App:
    https://play.google.com/store/apps/details?id=org.trecet.mypc
    Copyright (C) 2020    Pablo Fernandez    naevtamarkus@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import javax.swing.JPanel;

public abstract class KeyMap {

    public static enum KeyType { SHORT, LONG };

    abstract KeyCombination getKey(KeyType type, int num);

    abstract int size();

    abstract JPanel getInstructions();

}