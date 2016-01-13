/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* A set of predefined colors for the AML GUI.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.ui;

import java.awt.Color;

public class AMLColor extends Color
{
	private static final long serialVersionUID = -2597916025219295034L;
	public static final Color BLACK = new Color(20,20,20);
	public static final Color BLUE = new Color(20,60,120);
	public static final Color BROWN = new Color(120,120,30);
	public static final Color CYAN = new Color(100,220,250);
	public static final Color DARK_GRAY = new Color(170,170,170);
	public static final Color GRAY = new Color(200,200,200);
	public static final Color GREEN = new Color(100,180,100);
	public static final Color LIGHT_GRAY = new Color(230,230,230);
	public static final Color ORANGE = new Color(240,200,80);
	public static final Color RED = new Color(210,80,80);
	public static final Color WHITE = new Color(245,245,245);
	
	public AMLColor(int r, int g, int b)
	{
		super(r, g, b);
	}
}
