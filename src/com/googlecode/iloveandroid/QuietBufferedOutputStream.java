/**
 *   This file is part of I Love Android.
 *
 *   I Love Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   I Love Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with I Love Android.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.googlecode.iloveandroid;

import java.io.*;

/**
 * Android inserts a message into the log telling the developer to specify an 8K buffer size
 * for <code>BufferedOutputStream</code> even though its not needed. This class works 
 * the same as <code>BufferedOutputStream</code> but automatically specifies the 8K size so 
 * Android doesn't complain, i.e.: is "quiet." 
 * @author Eric Schultz
 *
 */
public class QuietBufferedOutputStream extends BufferedOutputStream
{
	public QuietBufferedOutputStream(OutputStream out)
	{
		super(out,8192);
	}

	public QuietBufferedOutputStream(OutputStream out, int size)
	{
		super(out, size);
	}

}
