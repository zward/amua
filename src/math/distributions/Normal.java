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

package math.distributions;

import math.MathUtils;
import math.Numeric;
import math.NumericException;

import org.apache.commons.math3.distribution.NormalDistribution;

import main.MersenneTwisterFast;

public final class Normal{
	
	public static Numeric pdf(Numeric params[]) throws NumericException{
		if(params[0].isMatrix()==false && params[1].isMatrix()==false && params[2].isMatrix()==false) { //real number
			double x=params[0].getDouble(), mu=params[1].getDouble(), sigma=params[2].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			NormalDistribution norm=new NormalDistribution(null,mu,sigma);
			return(new Numeric(norm.density(x)));
		}
		else { //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("x and μ should be the same size","Norm");}
			if(params[1].nrow!=params[2].nrow || params[1].ncol!=params[2].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double x=params[0].matrix[i][j];
					double mu=params[1].matrix[i][j];
					double sigma=params[2].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					NormalDistribution norm=new NormalDistribution(null,mu,sigma);
					vals.matrix[i][j]=norm.density(x);
				}
			}
			return(vals);
		}
	}

	public static Numeric cdf(Numeric params[]) throws NumericException{
		if(params[0].isMatrix()==false && params[1].isMatrix()==false && params[2].isMatrix()==false) { //real number
			double x=params[0].getDouble(), mu=params[1].getDouble(), sigma=params[2].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			NormalDistribution norm=new NormalDistribution(null,mu,sigma);
			return(new Numeric(norm.cumulativeProbability(x)));
		}
		else { //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("x and μ should be the same size","Norm");}
			if(params[1].nrow!=params[2].nrow || params[1].ncol!=params[2].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double x=params[0].matrix[i][j];
					double mu=params[1].matrix[i][j];
					double sigma=params[2].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					NormalDistribution norm=new NormalDistribution(null,mu,sigma);
					vals.matrix[i][j]=norm.cumulativeProbability(x);
				}
			}
			return(vals);
		}
	}	
	
	public static Numeric quantile(Numeric params[]) throws NumericException{
		if(params[0].isMatrix()==false && params[1].isMatrix()==false && params[2].isMatrix()==false) { //real number
			double x=params[0].getProb(), mu=params[1].getDouble(), sigma=params[2].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			NormalDistribution norm=new NormalDistribution(null,mu,sigma);
			return(new Numeric(norm.inverseCumulativeProbability(x)));
		}
		else { //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("x and μ should be the same size","Norm");}
			if(params[1].nrow!=params[2].nrow || params[1].ncol!=params[2].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double x=params[0].getMatrixProb(i, j);
					double mu=params[1].matrix[i][j];
					double sigma=params[2].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					NormalDistribution norm=new NormalDistribution(null,mu,sigma);
					vals.matrix[i][j]=norm.inverseCumulativeProbability(x);
				}
			}
			return(vals);
		}
	}
	
	public static Numeric mean(Numeric params[]) throws NumericException{
		if(params[0].isMatrix()==false && params[1].isMatrix()==false) { //real number
			double mu=params[0].getDouble(), sigma=params[1].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			return(new Numeric(mu));
		}
		else { //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double mu=params[0].matrix[i][j];
					double sigma=params[1].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					vals.matrix[i][j]=mu;
				}
			}
			return(vals);
		}
	}
	
	public static Numeric variance(Numeric params[]) throws NumericException{
		if(params[0].isMatrix()==false && params[1].isMatrix()==false) { //real number
			double sigma=params[1].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			return(new Numeric(sigma*sigma));
		}
		else { //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double mu=params[0].matrix[i][j];
					double sigma=params[1].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					vals.matrix[i][j]=sigma*sigma;
				}
			}
			return(vals);
		}
	}

	public static Numeric sample(Numeric params[], MersenneTwisterFast generator) throws NumericException{
		if(params.length!=2){
			throw new NumericException("Incorrect number of parameters","Norm");
		}
		if(params[0].isMatrix()==false && params[1].isMatrix()==false) { //real number
			double mu=params[0].getDouble(), sigma=params[1].getDouble();
			if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
			NormalDistribution norm=new NormalDistribution(null,mu,sigma);
			double rand=generator.nextDouble();
			return(new Numeric(norm.inverseCumulativeProbability(rand)));
		}
		else{ //matrix
			if(params[0].nrow!=params[1].nrow || params[0].ncol!=params[1].ncol) {throw new NumericException("μ and σ should be the same size","Norm");}
			int nrow=params[0].nrow; int ncol=params[0].ncol;
			Numeric vals=new Numeric(nrow,ncol); //create result matrix
			for(int i=0; i<nrow; i++) {
				for(int j=0; j<ncol; j++) {
					double mu=params[0].matrix[i][j];
					double sigma=params[1].matrix[i][j];
					if(sigma<=0){throw new NumericException("σ should be >0","Norm");}
					NormalDistribution norm=new NormalDistribution(null,mu,sigma);
					double rand=generator.nextDouble();
					vals.matrix[i][j]=norm.inverseCumulativeProbability(rand);
				}
			}
			return(vals);
		}
	}
	
	public static String description(){
		String des="<html><b>Normal Distribution</b><br>";
		des+="Canonical bell-shaped distribution<br><br>";
		des+="<i>Parameters</i><br>";
		des+=MathUtils.consoleFont("μ")+": Mean<br>";
		des+=MathUtils.consoleFont("σ")+": Standard deviation ("+MathUtils.consoleFont(">0") +")<br>";
		des+="<br><i>Sample</i><br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(μ,σ,<b><i>~</i></b>)")+": Returns a random variable (mean in base case) from the Normal distribution. Real number<br>";
		des+="<br><i>Distribution Functions</i><br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(x,μ,σ,<b><i>f</i></b>)")+": Returns the value of the Normal PDF at "+MathUtils.consoleFont("x")+"<br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(x,μ,σ,<b><i>F</i></b>)")+": Returns the value of the Normal CDF at "+MathUtils.consoleFont("x")+"<br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(x,μ,σ,<b><i>Q</i></b>)")+": Returns the quantile (inverse CDF) of the Normal distribution at "+MathUtils.consoleFont("x")+"<br>";
		des+="<i><br>Moments</i><br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(μ,σ,<b><i>E</i></b>)")+": Returns the mean of the Normal distribution<br>";
		des+=MathUtils.consoleFont("<b>Norm</b>","green")+MathUtils.consoleFont("(μ,σ,<b><i>V</i></b>)")+": Returns the variance of the Normal distribution<br>";
		des+="</html>";
		return(des);
	}
}