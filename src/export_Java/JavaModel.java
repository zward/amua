/**
 * Amua - An open source modeling framework.
 * Copyright (C) 2017 Zachary J. Ward
 *
 * This file is part of Amua. Amua is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Amua is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Amua.  If not, see <http://www.gnu.org/licenses/>.
 */

package export_Java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;

import base.AmuaModel;
import main.*;
import math.Constants;
import math.Distributions;
import math.Functions;
import math.Interpreter;
import math.MatrixFunctions;
import math.Numeric;
import math.NumericException;

public class JavaModel{
	String dir;
	BufferedWriter out;
	AmuaModel myModel;
	ArrayList<String> functionNames; //to export
	ArrayList<String> functionMethods;
		
	public JavaModel(String dir, BufferedWriter out, AmuaModel myModel){
		this.dir=dir;
		this.out=out;
		this.myModel=myModel;
		functionNames=new ArrayList<String>();
		functionMethods=new ArrayList<String>();
	}

	public void writeProperties(){
		try{
			writeLine("/*");
			writeLine("This code was auto-generated by Amua (https://github.com/zward/Amua)");
			writeLine("Code generated: "+new Date());
			writeLine("Model name: "+myModel.name);
			if(myModel.type==0){writeLine("Model type: Decision Tree");}
			else if(myModel.type==1){writeLine("Model type: Markov Model");}
			if(myModel.simType==0){writeLine("Simulation type: Cohort");}
			else if(myModel.simType==1){writeLine("Simulation type: Monte Carlo");}
			//metadata
			writeLine("Created by: "+myModel.meta.author);
			writeLine("Created: "+myModel.meta.dateCreated);
			writeLine("Version created: "+myModel.meta.versionCreated);
			writeLine("Modified by: "+myModel.meta.modifier);
			writeLine("Modified: "+myModel.meta.dateModified);
			writeLine("Version modified: "+myModel.meta.versionModified);
			writeLine("*/");
		}catch(Exception e){
			recordError(e);
		}
	}


	public String translate(String expression, boolean personLevel) throws Exception{
		String curText=expression.replaceAll(" ", ""); //remove spaces
		curText=curText.replaceAll("\'", "\""); //replace ' with "
		String exportText="";
		//Parse expression word by word
		int len=curText.length();
		while(len>0){
			int pos=Interpreter.getNextBreakIndex(curText);
			String word=curText.substring(0, pos);
			String split="";
			if(pos<len){split=curText.substring(pos,pos+1);}

			if(myModel.isTable(word)){ //if table
				int tableIndex=myModel.getTableIndex(word);
				Table curTable=myModel.tables.get(tableIndex);
				if(curTable.type.matches("Lookup")){
					int close=Interpreter.findRightBracket(curText,pos);
					String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
					exportText+=curTable.name+".getLookupValue("+translate(args[0],personLevel)+","+curTable.getColumnIndex(args[1])+")";
					pos=close; //Move to end of table indices
				}
				else if(curTable.type.matches("Distribution")){ //Replace with value
					int close=Interpreter.findRightParen(curText,pos);
					String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
					exportText+=curTable.name+".calcEV("+curTable.getColumnIndex(args[0])+")";
					pos=close; //Move to end of dist parameters
				}
				else if(curTable.type.matches("Matrix")){
					if(pos<len && curText.charAt(pos)=='['){ //matrix index
						int close=Interpreter.findRightBracket(curText,pos);
						String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
						exportText+=curTable.name+"data["+translate(args[0],personLevel)+"]["+translate(args[1],personLevel)+"]";
						pos=close; //Move to end of matrix indices
					}
					else{ //entire matrix
						exportText+=curTable.name;
					}
				}
			}
			else if(myModel.isVariable(word)){ //Variable
				if(personLevel){ //individual-level
					exportText+="curPerson."+word+split;
				}
				else{
					exportText+=word+split;
				}
			}
			else if(word.matches("trace")){ //Markov Trace
				int close=Interpreter.findRightBracket(curText,pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				exportText+="trace.getValue("+translate(args[0],personLevel)+","+translate(args[1],personLevel)+")";
				pos=close; //Move to end of trace indices
			}
			else if(Functions.isFunction(word)){
				int close=Interpreter.findRightParen(curText, pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				//translate function call
				exportText+=JavaFunctions.translate(word)+"("+translate(args[0],personLevel);
				for(int i=1; i<args.length; i++){exportText+=","+translate(args[i],personLevel);}
				exportText+=")";
				pos=close+1; //Move to end of function call
				if(JavaFunctions.inPlace(word)==false){ //add function method
					if(!functionNames.contains(word)){ //not defined yet
						functionNames.add(word);
						functionMethods.add(JavaFunctions.define(word));
					}
				}
			}
			else if(MatrixFunctions.isFunction(word)){
				//just parse arguments for now
				int close=Interpreter.findRightParen(curText, pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				exportText+=word+"("+translate(args[0],personLevel);
				for(int i=1; i<args.length; i++){
					exportText+=","+translate(args[i],personLevel);
				}
				exportText+=")";
				pos=close+1; //Move to end of function call
			}
			else if(Distributions.isDistribution(word)){
				//just parse arguments for now
				int close=Interpreter.findRightParen(curText,pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				exportText+=word+"("+translate(args[0],personLevel);
				for(int i=1; i<args.length; i++){
					exportText+=","+translate(args[i],personLevel);
				}
				exportText+=")";
				pos=close+1; //Move to end of distribution call
			}
			else if(Constants.isConstant(word)){
				exportText+=JavaConstants.translate(word)+split;
			}
			else if(curText.charAt(0)=='['){ //matrix
				int close=Interpreter.findRightBracket(curText,0);
				String strMatrix=curText.substring(1,close);
				Numeric matrix=Interpreter.parseMatrix(strMatrix,myModel).getNumeric()[0];
				//write out
				if(matrix.nrow>1){exportText+="new double[][]"+writeMatrix(matrix.matrix);}
				else{exportText+="new double[]"+writeArray(matrix.matrix[0]);}
				pos=close+1; //Move to end of matrix
			}
			
			else{ //not key word
				exportText+=word+split;
			}

			if(pos==len){len=0;} //End of word
			else{
				curText=curText.substring(pos+1);
				len=curText.length();
			}
		}

		return(exportText);
	}

	public String defNumeric(String name, Numeric value){
		String def="";
		if(value.isDouble()){def="double "+name+";";}
		else if(value.isInteger()){def="double "+name+";";} //initialize as double anyways
		else if(value.isBoolean()){def="boolean "+name+";";}
		else if(value.isMatrix()){def="double[][] "+name+";";}
		return(def);
	}

	private String initNumeric(String name, Numeric value) throws NumericException{
		String init="";
		if(value.isDouble()){init="		double "+name+"="+value.getDouble()+";";}
		else if(value.isInteger()){init="		double "+name+"="+value.getInt()+";";} //initialize as double anyways
		else if(value.isBoolean()){init="		boolean "+name+"="+value.getBool()+";";}
		else if(value.isMatrix()){init="		double[][] "+name+"="+writeMatrix(value.matrix)+";";}
		return(init);
	}


	public void writeParameters() throws NumericException{
		int numParams=myModel.parameters.size();
		if(numParams>0){
			writeLine("		//Define parameters");
			for(int i=0; i<numParams; i++){
				Parameter curParam=myModel.parameters.get(i);
				if(!curParam.notes.isEmpty()){
					writeLine("		/*"+curParam.notes+"*/");
				}
				String expr=curParam.expression;
				String init=initNumeric(curParam.name,curParam.value);
				writeLine(init+" //Expression: "+expr);
			}
			writeLine("");
		}
	}

	public void writeVariables() throws NumericException{
		int numVars=myModel.variables.size();
		if(numVars>0){
			writeLine("		//Define variables");
			for(int i=0; i<numVars; i++){
				Variable curVar=myModel.variables.get(i);
				if(!curVar.notes.isEmpty()){
					writeLine("		/*"+curVar.notes+"*/");
				}
				String init=initNumeric(curVar.name,curVar.value[0]);
				writeLine(init);
			}
			writeLine("");
		}
	}

	public void writeTables(int format){
		try{
			int numTables=myModel.tables.size();
			if(numTables>0){
				writeLine("		//Define tables");
				if(format==0){ //In-line
					for(int i=0; i<myModel.tables.size(); i++){
						Table curTable=myModel.tables.get(i);
						if(!curTable.notes.isEmpty()){out.write("/* "+curTable.notes+"*/"); out.newLine();}
						out.write("		String[] headers_"+curTable.name+"=new String[]{");
						for(int c=0; c<curTable.numCols-1; c++){out.write("\""+curTable.headers[c]+"\",");}
						out.write("\""+curTable.headers[curTable.numCols-1]+"\"};"); out.newLine();
						writeLine("		double [][] data_"+curTable.name+"=new double["+curTable.numRows+"][];");
						for(int r=0; r<curTable.numRows; r++){
							out.write("		data_"+curTable.name+"["+r+"]=new double[]{");
							for(int c=0; c<curTable.numCols-1; c++){out.write(curTable.data[r][c]+",");}
							writeLine(curTable.data[r][curTable.numCols-1]+"};");
						}
						//define table object
						out.write("		Table "+curTable.name+"=new Table(\""+curTable.name+"\","); //Name
						out.write("\""+curTable.type+"\",");
						out.write("\""+curTable.lookupMethod+"\",");
						out.write("\""+curTable.interpolate+"\",");
						out.write("\""+curTable.boundary+"\",");
						out.write("\""+curTable.extrapolate+"\",");
						out.write("headers_"+curTable.name+", data_"+curTable.name+");"); out.newLine(); //Headers + data
						if(curTable.interpolate!=null && curTable.interpolate.matches("Cubic Splines")){
							writeTableSplines(curTable);
						}
						out.newLine();
					}
				}
				else if(format==1){ //CSV
					writeLine("		String dir=\""+dir.replaceAll("\\\\", "\\\\\\\\")+"\"+File.separator;");
					for(int i=0; i<myModel.tables.size(); i++){
						Table curTable=myModel.tables.get(i);
						//Write out table
						String curPath=dir+curTable.name+".csv";
						curTable.writeCSV(curPath, myModel.errorLog);
						//construct table object
						if(!curTable.notes.isEmpty()){out.write("/* "+curTable.notes+"*/"); out.newLine();}
						out.write("		Table "+curTable.name+"=new Table(\""+curTable.name+"\","); //Name
						out.write("\""+curTable.type+"\",");
						out.write("\""+curTable.lookupMethod+"\",");
						out.write("\""+curTable.interpolate+"\",");
						out.write("\""+curTable.boundary+"\",");
						out.write("\""+curTable.extrapolate+"\",");
						out.write(curTable.numRows+", "+curTable.numCols+", dir+\""+curTable.name+".csv\");"); out.newLine();
						if(curTable.interpolate!=null && curTable.interpolate.matches("Cubic Splines")){
							writeTableSplines(curTable);
						}
						out.newLine();
					}
					out.newLine();

				}
			}
		}catch(Exception e){
			recordError(e);
		}
	}

	private void writeTableSplines(Table curTable) throws IOException{
		int numSplines=curTable.splines.length;
		out.write("		"+curTable.name+".splines=new Table.CubicSpline["+numSplines+"];"); out.newLine();
		for(int s=0; s<numSplines; s++){
			out.write("		"+curTable.name+".splines["+s+"]="+curTable.name+".new CubicSpline();"); out.newLine();
			out.write("		"+curTable.name+".splines["+s+"].numSplines="+curTable.splines[s].numSplines+";"); out.newLine();
			out.write("		"+curTable.name+".splines["+s+"].knots=new double[]"+writeArray(curTable.splines[s].knots)+";"); out.newLine();
			out.write("		"+curTable.name+".splines["+s+"].knotHeights=new double[]"+writeArray(curTable.splines[s].knotHeights)+";"); out.newLine();
			out.write("		"+curTable.name+".splines["+s+"].splineCoeffs=new double[][]"+writeMatrix(curTable.splines[s].splineCoeffs)+";"); out.newLine();
			out.write("		"+curTable.name+".splines["+s+"].boundaryCondition="+curTable.splines[s].boundaryCondition+";"); out.newLine();
		}
	}

	private String writeArray(double array[]){
		int len=array.length;
		String write="{";
		for(int i=0; i<len-1; i++){
			write+=array[i]+",";
		}
		write+=array[len-1]+"}";
		return(write);
	}

	private String writeMatrix(double matrix[][]){
		int nrow=matrix.length, ncol=matrix[0].length;
		String write="{\n";
		for(int r=0; r<nrow-1; r++){
			write+="			{";
			for(int c=0; c<ncol-1; c++){
				write+=matrix[r][c]+",";
			}
			write+=matrix[r][ncol-1]+"},\n";
		}
		//last row
		write+="			{";
		for(int c=0; c<ncol-1; c++){
			write+=matrix[nrow-1][c]+",";
		}
		write+=matrix[nrow-1][ncol-1]+"}\n";
		write+="		}";
		return(write);
	}

	/**
	 * Defines any function methods needed
	 */
	public void defineFunctions(){
		try{
			int numFx=functionNames.size();
			for(int f=0; f<numFx; f++){
				out.write(functionMethods.get(f)); out.newLine();
				out.newLine();
			}
		}catch(Exception e){
			recordError(e);
		}
	}
	
	/**
	 * Defines a table class in Java
	 * @param out
	 * @throws IOException 
	 */
	public void writeTableClass() throws IOException{
		int numTables=myModel.tables.size();
		if(numTables>0){
			FileWriter fstream = new FileWriter(dir+"Table.java"); //Create new file
			BufferedWriter outTable = new BufferedWriter(fstream);
			BufferedWriter origOut=out;
			out=outTable; //re-point

			writeProperties();
			writeLine("");
			writeLine("import java.io.BufferedReader;");
			writeLine("import java.io.DataInputStream;");
			writeLine("import java.io.FileInputStream;");
			writeLine("import java.io.InputStreamReader;");
			writeLine("");
			writeLine("public class Table{");
			writeLine("	//Attributes");
			writeLine("	String name;");
			writeLine("	String type;");
			writeLine("	String lookupMethod;");
			writeLine("	String interpolate;");
			writeLine("	String boundary;");
			writeLine("	String extrapolate;");
			writeLine("	int numRows, numCols;");
			writeLine("	String headers[];");
			writeLine("	double data[][];");
			writeLine("	CubicSpline splines[];");
			writeLine("");
			writeLine("	//Constructor - inline");
			writeLine("	public Table(String name, String type, String lookupMethod, String interpolate, String boundary, String extrapolate, String headers[], double data[][]){");
			writeLine("		this.name=name;");
			writeLine("		this.type=type;");
			writeLine("		this.lookupMethod=lookupMethod;");
			writeLine("		this.interpolate=interpolate;");
			writeLine("		this.boundary=boundary;");
			writeLine("		this.extrapolate=extrapolate;");
			writeLine("		this.headers=headers;");
			writeLine("		this.data=data;");
			writeLine("		this.numRows=data.length;");
			writeLine("		this.numCols=headers.length;");
			writeLine("	}");
			writeLine("");
			writeLine("	//Constructor - read csv");
			writeLine("	public Table(String name, String type, String lookupMethod, String interpolate, String boundary, String extrapolate, int numRows, int numCols, String filepath){");
			writeLine("		try{");
			writeLine("			this.name=name;");
			writeLine("			this.type=type;");
			writeLine("			this.lookupMethod=lookupMethod;");
			writeLine("			this.interpolate=interpolate;");
			writeLine("			this.boundary=boundary;");
			writeLine("			this.extrapolate=extrapolate;");
			writeLine("			this.numCols=numCols;");
			writeLine("			this.numRows=numRows;");
			writeLine("			FileInputStream fstream=new FileInputStream(filepath);");
			writeLine("			DataInputStream in=new DataInputStream(fstream);");
			writeLine("			BufferedReader br=new BufferedReader(new InputStreamReader(in));");
			writeLine("			String strLine=br.readLine(); //Headers");
			writeLine("			this.headers=strLine.split(\",\");");
			writeLine("			this.data=new double[numRows][numCols];");
			writeLine("			for(int r=0; r<numRows; r++){");
			writeLine("				strLine=br.readLine();");
			writeLine("				String curRow[]=strLine.split(\",\");");
			writeLine("				for(int c=0; c<numCols; c++){");
			writeLine("					this.data[r][c]=Double.parseDouble(curRow[c]);");
			writeLine("				}");
			writeLine("			}");
			writeLine("			br.close();");
			writeLine("		}catch(Exception e){");
			writeLine("			e.printStackTrace();");
			writeLine("		}");
			writeLine("	}");
			writeLine("");
			writeLine("	public double getLookupValue(double index, int col){");
			writeLine("		if(col<1 || col>(numCols-1)){return(Double.NaN);} //Invalid column");
			writeLine("		double val=Double.NaN;");
			writeLine("		if(lookupMethod.matches(\"Exact\")){");
			writeLine("			int row=-1;");
			writeLine("			boolean found=false;");
			writeLine("			while(found==false && row<numRows){");
			writeLine("				row++;");
			writeLine("				if(index==data[row][0]){found=true;}");
			writeLine("			}");
			writeLine("			if(found){val=data[row][col];}");
			writeLine("		}");
			writeLine("		else if(lookupMethod.matches(\"Truncate\")){");
			writeLine("			if(index<data[0][0]){val=Double.NaN;} //Below first value - error");
			writeLine("			else if(index>=data[numRows-1][0]){val=data[numRows-1][col];} //Above last value");
			writeLine("			else{");
			writeLine("				int row=0;");
			writeLine("				while(data[row][0]<index){row++;}");
			writeLine("				if(index==data[row][0]){val=data[row][col];}");
			writeLine("				else{val=data[row-1][col];}");
			writeLine("			}");
			writeLine("		}");
			writeLine("		else if(lookupMethod.matches(\"Interpolate\")){");
			writeLine("			if(interpolate.matches(\"Linear\")){");
			writeLine("				if(index<=data[0][0]){ //Below or at first index");
			writeLine("					double slope=(data[1][col]-data[0][col])/(data[1][0]-data[0][0]);");
			writeLine("					val=data[0][col]-(data[0][0]-index)*slope;");
			writeLine("				}");
			writeLine("				else if(index>data[numRows-1][0]){ //Above last index");
			writeLine("					double slope=(data[numRows-1][col]-data[numRows-2][col])/(data[numRows-1][0]-data[numRows-2][0]);");
			writeLine("					val=data[numRows-1][col]+(index-data[numRows-1][0])*slope;");
			writeLine("				}");
			writeLine("				else{ //Between");
			writeLine("					int row=0;");
			writeLine("					while(data[row][0]<index){row++;}");
			writeLine("					double slope=(data[row][col]-data[row-1][col])/(data[row][0]-data[row-1][0]);");
			writeLine("					val=data[row-1][col]+(index-data[row-1][0])*slope;");
			writeLine("				}");
			writeLine("			}");
			writeLine("			else if(interpolate.matches(\"Cubic Splines\")){");
			writeLine("				val=splines[col-1].evaluate(index);");
			writeLine("			}");
			writeLine("");			
			writeLine("			//Check extrapolation conditions");
			writeLine("			if(extrapolate.matches(\"No\")){");
			writeLine("				if(index<=data[0][0]){val=data[0][col];} //Below or at first index");
			writeLine("				else if(index>data[numRows-1][0]){val=data[numRows-1][col];} //Above last index");
			writeLine("			}");
			writeLine("			else if(extrapolate.matches(\"Left only\")){ //truncate right");
			writeLine("				if(index>data[numRows-1][0]){val=data[numRows-1][col];} //Above last index");
			writeLine("			}");
			writeLine("			else if(extrapolate.matches(\"Right only\")){ //truncate left");
			writeLine("				if(index<=data[0][0]){val=data[0][col];} //Below or at first index");
			writeLine("			}");
			writeLine("		}");
			writeLine("");
			writeLine("		return(val);");
			writeLine("	}");
			writeLine("");
			writeLine("	public double calcEV(int col){");
			writeLine("		double ev=0;");
			writeLine("		for(int r=0; r<numRows; r++){");
			writeLine("			ev+=data[r][0]*data[r][col];");
			writeLine("		}");
			writeLine("		return(ev);");
			writeLine("	}");
			writeLine("");
			writeLine("	//Define inner class");
			writeLine("	class CubicSpline{");
			writeLine("		//Attributes");
			writeLine("		double knots[];");
			writeLine("		double knotHeights[];");
			writeLine("		int numSplines;");
			writeLine("		/**");
			writeLine("		* [Spline #][Coeff]");
			writeLine("		*/");
			writeLine("		double splineCoeffs[][];");
			writeLine("		/**");
			writeLine("		* 0=Natural, 1=Clamped, 2=Not-a-knot, 3=Periodic");
			writeLine("		*/");
			writeLine("		int boundaryCondition;");
			writeLine("");
			writeLine("		//Constructor");
			writeLine("		public CubicSpline(){");
			writeLine("		}");
			writeLine("");
			writeLine("		public double evaluate(double x){");
			writeLine("			double y=Double.NaN;");
			writeLine("			//Find domain");
			writeLine("			int index=-1;");
			writeLine("			if(x<knots[0]){ //Extrapolate left");
			writeLine("				x=x-knots[0];");
			writeLine("				double a[]=splineCoeffs[0];");
			writeLine("				if(boundaryCondition==0 || boundaryCondition==1){ //Natural or clamped");
			writeLine("					double slope=a[1];");
			writeLine("					y=slope*x+knotHeights[0];");
			writeLine("				}");
			writeLine("				else{ //Not-a-knot or periodic");
			writeLine("					index=0;");
			writeLine("					y=splineCoeffs[index][0]+splineCoeffs[index][1]*x+splineCoeffs[index][2]*x*x+splineCoeffs[index][3]*x*x*x;");
			writeLine("				}");
			writeLine("			}");
			writeLine("			else if(x>knots[numSplines]){ //Extrapolate right");
			writeLine("				double a[]=splineCoeffs[numSplines-1];");
			writeLine("				if(boundaryCondition==0 || boundaryCondition==1){ //Natural or clamped");
			writeLine("					x=x-knots[numSplines];");
			writeLine("					double h=knots[numSplines]-knots[numSplines-1];");
			writeLine("					double slope=a[1]+2*a[2]*h+3*a[3]*h*h;");
			writeLine("					y=slope*x+knotHeights[numSplines];");
			writeLine("				}");
			writeLine("				else{ //Not-a-knot or periodic");
			writeLine("					index=numSplines-1;");
			writeLine("					x=x-knots[index];");
			writeLine("					y=splineCoeffs[index][0]+splineCoeffs[index][1]*x+splineCoeffs[index][2]*x*x+splineCoeffs[index][3]*x*x*x;");
			writeLine("				}");
			writeLine("			}");
			writeLine("			else{ //Interpolate");
			writeLine("				index=0;");
			writeLine("				while(x>knots[index+1] && index<numSplines-1){index++;}");
			writeLine("				x=x-knots[index];");
			writeLine("				y=splineCoeffs[index][0]+splineCoeffs[index][1]*x+splineCoeffs[index][2]*x*x+splineCoeffs[index][3]*x*x*x;");
			writeLine("			}");
			writeLine("			return(y);");
			writeLine("		}");
			writeLine("	}"); //end CublicSpline class
			writeLine("}"); //end Table class
			out.close();

			out=origOut; //point back
		}
	}

	public void writeMarkovTrace(){
		try{
			String dimNames[]=myModel.dimInfo.dimNames;
			int numDimensions=dimNames.length;
			writeLine("	//Define inner class");
			writeLine("	class MarkovTrace{");
			writeLine("		//Attributes");
			writeLine("		String name;");
			writeLine("		int numStates;");
			writeLine("		int numCols;");
			writeLine("		ArrayList<String> headers;");
			writeLine("		ArrayList<double[]> data;");
			writeLine("");
			writeLine("		//Constructor");
			writeLine("		public MarkovTrace(String name, String stateNames[]){");
			writeLine("			this.name=name;");
			writeLine("			numStates=stateNames.length;");
			writeLine("			headers=new ArrayList<String>();");
			writeLine("			headers.add(\"Cycle\");");
			writeLine("			for(int s=0; s<stateNames.length; s++){");
			writeLine("				headers.add(stateNames[s]);");
			writeLine("			}");
			for(int d=0; d<numDimensions; d++){
				writeLine("			headers.add(\"Cycle_"+dimNames[d]+"\");");
				writeLine("			headers.add(\"Cum_"+dimNames[d]+"\");");
				writeLine("			headers.add(\"Cycle_Dis_"+dimNames[d]+"\");");
				writeLine("			headers.add(\"Cum_Dis_"+dimNames[d]+"\");");
			}
			writeLine("			numCols=headers.size();");
			writeLine("			data=new ArrayList<double[]>();");
			writeLine("		}");
			writeLine("");
			out.write("		public void update(double prev[]");
			for(int d=0; d<numDimensions; d++){out.write(", double "+dimNames[d]+", double "+dimNames[d]+"_dis");}
			out.write("){"); out.newLine();
			writeLine("			double row[]=new double[numCols];");
			writeLine("			int cycle=data.size();");
			writeLine("			double prevRow[]=null;");
			writeLine("			if(cycle>0){prevRow=data.get(cycle-1);}");
			writeLine("			row[0]=cycle;");
			writeLine("			int col=1;");
			writeLine("			for(int s=0; s<prev.length; s++){");
			writeLine("				row[col]=prev[s];");
			writeLine("				col++;");
			writeLine("			}");
			for(int d=0; d<numDimensions; d++){
				writeLine("			row[col]="+dimNames[d]+"; col++; //cycle reward");
				writeLine("			row[col]="+dimNames[d]+";");
				writeLine("			if(cycle>0){row[col]+=prevRow[col];} //cum reward");
				writeLine("			col++;");
				writeLine("			row[col]="+dimNames[d]+"_dis; col++; //discounted cycle reward");
				writeLine("			row[col]="+dimNames[d]+"_dis;");
				writeLine("			if(cycle>0){row[col]+=prevRow[col];} //discounted cum reward");
				writeLine("			col++;");
			}
			writeLine("			data.add(row);");
			writeLine("		}");
			writeLine("");
			writeLine("		public void applyHalfCycle(){");
			writeLine("			double row[]=data.get(data.size()-1); //last row");
			writeLine("			int col=numStates+1;");
			writeLine("			double half;");
			for(int d=0; d<numDimensions; d++){
				writeLine("			half=row[col]*0.5;");
				writeLine("			row[col]=half; col++; //cycle reward");
				writeLine("			row[col]-=half; col++; //cum reward");
				writeLine("			half=row[col]*0.5;");
				writeLine("			row[col]=half; col++; //discounted cycle reward");
				writeLine("			row[col]-=half; col++; //discounted cum reward");
				writeLine("			col++;");
			}
			writeLine("		}");
			writeLine("");
			writeLine("		public double getValue(int t, int col){");
			writeLine("			double val=data.get(t)[col];");
			writeLine("			return(val);");
			writeLine("		}");
			writeLine("");
			writeLine("		public double getValue(int t, String colName){");
			writeLine("			int col=headers.indexOf(colName);");
			writeLine("			double val=data.get(t)[col];");
			writeLine("			return(val);");
			writeLine("		}");
			writeLine("");
			writeLine("		public void writeCSV(){");
			writeLine("			try{");
			writeLine("				String dir=\""+dir.replaceAll("\\\\", "\\\\\\\\")+"\";");
			writeLine("				FileWriter fstream = new FileWriter(dir+name+\"_Trace.csv\");");
			writeLine("				BufferedWriter out = new BufferedWriter(fstream);");
			writeLine("				//Headers");
			writeLine("				for(int i=0; i<numCols-1; i++){");
			writeLine("					out.write(headers.get(i)+\",\");");
			writeLine("				}");
			writeLine("				out.write(headers.get(numCols-1)); out.newLine();");
			writeLine("				//Data");
			writeLine("				int numRows=data.size();");
			writeLine("				for(int i=0; i<numRows; i++){");
			writeLine("					double row[]=data.get(i);");
			writeLine("					for(int j=0; j<numCols-1; j++){");
			writeLine("						out.write(row[j]+\",\");");
			writeLine("					}");
			writeLine("					out.write(row[numCols-1]+\"\"); out.newLine();");
			writeLine("				}");
			writeLine("				out.close();");
			writeLine("			} catch (Exception e){");
			writeLine("				e.printStackTrace();");
			writeLine("			}");
			writeLine("		}");
			writeLine("	}"); //end markovTrace
		}catch(Exception e){
			recordError(e);
		}
	}

	private void writeLine(String line){
		try{
			out.write(line); out.newLine();
		}catch(Exception e){
			recordError(e);
		}
	}
	
	private void recordError(Exception e){
		e.printStackTrace();
		myModel.errorLog.recordError(e);
		JOptionPane.showMessageDialog(null, e.toString());
	}
}
