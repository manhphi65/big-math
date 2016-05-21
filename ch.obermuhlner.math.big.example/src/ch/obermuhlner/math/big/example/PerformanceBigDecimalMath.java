package ch.obermuhlner.math.big.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import ch.obermuhlner.math.big.BigDecimalMath;

/**
 * finding optimium point between 2 and 3
 * // x / 2 - 1 = 1 - x / 3
 *
 */
public class PerformanceBigDecimalMath {

	private static MathContext REF_MATHCONTEXT = new MathContext(300);
	
	private static int REPEATS = 10;
	
	private static final String OUTPUT_DIRECTORY = "docu/benchmarks/";

	public static void main(String[] args) {

//				System.out.println(BigDecimalMath.log(BigDecimal.valueOf(3), new MathContext(1100)));

				performanceReportStandardFunctions();
				performanceReportSlowFunctions();
				performanceReportVerySlowFunctions();
				performanceReportVerySlowFunctionBigRange();

//				performanceReportExp();
//				performanceReportLogBigRange();

//				performanceReportOverPrecision();

		// --- log() optimizations:
//				performanceReportLogOptimization1();
//				performanceReportLogOptimization2();
//				performanceReportLogOptimization3();
//				performanceReportLogOptimization4();
//				performanceReportLogOptimization5();
//				performanceReportLogOptimization6();
	}

	private static void performanceReportStandardFunctions() {
		performanceReportOverValue(
				"perf_standard_funcs_from_0_to_2.csv",
				REF_MATHCONTEXT,
				0,
				+2.0,
				+0.01,
				REPEATS,
				Arrays.asList("exp", "sqrt", "root2", "root3", "sin", "cos"),
				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.sqrt(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.root(new BigDecimal(2), x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.root(new BigDecimal(3), x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.sin(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.cos(x, calculationMathContext));
	}

	private static void performanceReportSlowFunctions() {
		performanceReportOverValue(
				"perf_slow_funcs_from_0_to_2.csv",
				REF_MATHCONTEXT,
				+0.01,
				+2.0,
				+0.01,
				REPEATS,
				Arrays.asList("exp", "log", "log2", "log10"),
				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log2(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log10(x, calculationMathContext));
	}

	private static void performanceReportVerySlowFunctions() {
		performanceReportOverValue(
				"perf_slow_funcs_from_0_to_2.csv",
				REF_MATHCONTEXT,
				+0.01,
				+2.0,
				+0.01,
				REPEATS,
				Arrays.asList("exp", "log", "pow"),
				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.pow(BigDecimal.valueOf(123.456), x, calculationMathContext));
	}

	private static void performanceReportVerySlowFunctionBigRange() {
		performanceReportOverValue(
				"perf_slow_funcs_from_-10_to_10.csv",
				REF_MATHCONTEXT,
				-10.0,
				+10.0,
				+0.1,
				REPEATS,
				Arrays.asList("exp", "log", "pow"),
				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.pow(BigDecimal.valueOf(123.456), x, calculationMathContext));
	}

	private static void performanceReportLogBigRange() {
		performanceReportOverValue(
				"perf_slow_funcs_from_0_to_100.csv",
				REF_MATHCONTEXT,
				+1,
				+100,
				+1,
				REPEATS,
				Arrays.asList("exp", "log", "pow"),
				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.log(x, calculationMathContext),
				(x, calculationMathContext) -> BigDecimalMath.pow(BigDecimal.valueOf(123.456), x, calculationMathContext));
	}

	/**
Writing an implementation of the binary logarithm function with BigDecimal is surprisingly challenging.

The first step is to decide between the basic approach.
Here are the two basic formulas I found that converge reasonably fast:
Hyperbolic tangent area:
CODE logHyperbolic

Newton's method:
CODE logNewton

The following graph shows the time in nanoseconds necessary to calculate the log(x) to a precision of 300 significant digits.
CHART hyperbolic, newton x=0.01 - 10

The hyperbolic tangent area approach wins hands down.

Both approaches have in common that they converge much faster when x is close to 1.0 and become increasingly slower the farther away we get from 1.0.  
CHART hyperbolic x=0.01 - 10
CHART hyperbolic x=0.1 - 100

In order to optimize this calculation we must bring the argument x closer to 1.0 and correct this outside of the logarithm in the appropriate manner.
The Wikipedia article http://en.wikipedia.org/wiki/Natural_logarithm mentions the following trick:
We write the value x in the exponential form
	x = a * 10<sup>b</sup>
then we can say that
	log(x) = log(a) + log(10) * b
Since we can precalculate log(10) for reasonable precision this can be implemented very efficiently with BigDecimal.

CODE logUsingExponent

CHART logHyperbolic, logUsingExponent x=0.1 - 100

The argument of the log() can now only be in the range from 0.1 to 10, which is already an improvement.
How can we further reduce the input range of log() and get even closer to the optimum value of 1.0?

For values > 1.0 we can try to divide by powers of 2.
For example the worst case values in the range from 6.0 to 10.0 can be divided by (2*2*2), which gives us a range from 0.75 to 1.25, another improvement.
This factor can be applied outside the log() function:
 log(10.0) = log(10.0 / 8) + log(2)*3
To be efficient we must precalculate log(2).

The same logic applies to values > 1.0 but in this case we multiply with powers of 2.
 log(0.1) = log(0.1 * 8) - log(2)*3

CODE logUsingPowerTwo

CHART logUsingExponent, logUsingPowerTwo  x=0.1 - 2

The powers of 2 come in a pretty good sequence in the value range between 1.0 and 10.0: (2, 4, 8).
Unfortunately this leaves a gaping whole around 6.0 so that the values in this area can not be efficiently optimized.
If we do the same trick by multiplying combinations of the prim factors 2 and 3 we get a nicer sequence that leaves less holes:
(2, 3, 2*2, 2*3, 2*2*2, 3*3) = (2, 3, 4, 6, 8, 9)
While this becomes a bit more complex to implement that just the powers of 2 it is still simple enough that we can hardcode all the combinations. 

CHART logUsingExponent, logUsingTwoThree  x=0.1 - 2


Another approach that does not need all the tricks with exponential form and combinations of 2 and 3.
REFERENCE https://github.com/miraclefoxx/bigdecimal-math/blob/master/src/main/java/io/github/miraclefoxx/math/BigDecimalMath.java
This needs another function root(n, x) which calculates the n'th root of the value x.
It follows the same recipe of bringing the argument of the log() function closer to 1 by writing it in this form:
 log(x) = log(root(r, x)^r) = r * log(root(r, x))
By estimating r correctly we can bring the argument to log() reasonably close.

CODE  root
CODE  logUsingRoot

CHART logHyperbolic, logUsingRoot  x=0.1 - 10

Looks very nice, the calculation becomes essentially independent of the distance to 1.0.

Lets compare the two approaches (assuming precalculated log(10), log(2), log(3)):
CHART logUsingRoot, logUsingTwoThree  x=0.1 - 10

Nobody stops us from combining the two approaches, we calculate the final step
(after using exponential form and combinations of 2 and 3) calling logRoot().
This gives us:
CHART logUsingRoot, logUsingTwoThreeRoot  x=0.1 - 10

If we cannot precalculate log(10), log(2) and  log(3) (or the desired precision is outside the precalculated range)
then the root() approach becomes more efficient: 
CHART logUsingRoot, logUsingTwoThreeNotPrecalculated  x=0.1 - 10

This gives us the final solution:
If we are inside the precalculated precision we use logUsingTwoThreeRoot, otherwise logUsingRoot.

REFERENCE github eobermuhlner/big-math
	 */
//	private static void performanceReportLogOptimization1() {
//		MathContext mathContext = new MathContext(300);
//
//		printHeaders("x", "HyperbolicTangent", "Newton");
//		printHeaders("number", "number", "number");
//		performanceReportOverValue(
//				mathContext,
//				0.01,
//				10,
//				0.01,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logAreaHyperbolicTangent(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingNewton(x, calculationMathContext));
//	}
//
//	private static void performanceReportLogOptimization2() {
//		MathContext mathContext = new MathContext(300);
//
//		printHeaders("x", "HyperbolicTangent", "Exponent+Hyperbolic", "Root");
//		performanceReportOverValue(
//				mathContext,
//				0.01,
//				10,
//				+0.01,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logAreaHyperbolicTangent(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingExponentAndAreaHyperbolicTangent(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingRoot(x, calculationMathContext));
//	}
//
//	private static void performanceReportLogOptimization3() {
//		MathContext mathContext = new MathContext(300);
//
//		printHeaders("x", "Hyperbolic", "ExponentHyperbolic", "Root");
//		performanceReportOverValue(
//				mathContext,
//				0.05,
//				100,
//				+0.05,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logAreaHyperbolicTangent(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingExponentAndAreaHyperbolicTangent(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingRoot(x, calculationMathContext));
//	}
//
//	private static void performanceReportLogOptimization4() {
//		MathContext mathContext = new MathContext(300);
//
//		printHeaders("x", "PowerTwo", "UsingRoot");
//		performanceReportOverValue(
//				mathContext,
//				0.01,
//				10,
//				+0.01,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingPowerTwo(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingRoot(x, calculationMathContext));
//	}
//
//	private static void performanceReportLogOptimization5() {
//		MathContext mathContext = new MathContext(200);
//
//		printHeaders("x", "TwoThree", "UsingRoot");
//		performanceReportOverValue(
//				mathContext,
//				0.1,
//				2,
//				+0.001,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingTwoThree(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingRoot(x, calculationMathContext));
//	}
//
//	private static void performanceReportLogOptimization6() {
//		MathContext mathContext = new MathContext(300);
//
//		printHeaders("x", "ExpPowerTwo", "UsingRoot");
//		performanceReportOverValue(
//				mathContext,
//				0.02,
//				50,
//				+0.02,
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingExponentPowerTwo(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMathExperimental.logUsingRoot(x, calculationMathContext));
//	}
//
//	private static void performanceReportOverPrecision() {
//		printHeaders("precision", "exp", "log", "pow");
//		printHeaders("number", "number", "number", "number");
//		performanceReportOverPrecision(
//				BigDecimal.valueOf(3.1),
//				10,
//				1000,
//				10,
//				(x, calculationMathContext) -> BigDecimalMath.exp(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMath.log(x, calculationMathContext),
//				(x, calculationMathContext) -> BigDecimalMath.pow(BigDecimal.valueOf(123.456), x, calculationMathContext));
//	}

	@SafeVarargs
	private static void performanceReportOverValue(String name, MathContext mathContext, double xStart, double xEnd, double xStep, int repeats, List<String> functionNames, BiFunction<BigDecimal, MathContext, BigDecimal>... calculations) {
		performanceReportOverValue(name, mathContext, BigDecimal.valueOf(xStart), BigDecimal.valueOf(xEnd), BigDecimal.valueOf(xStep), repeats, functionNames, calculations);
	}

	@SafeVarargs
	private static void performanceReportOverValue(String name, MathContext mathContext, BigDecimal xStart, BigDecimal xEnd, BigDecimal xStep, int repeats, List<String> functionNames, BiFunction<BigDecimal, MathContext, BigDecimal>... calculations) {
		//PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));
		
		System.out.println("Writing  " + name);
		try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_DIRECTORY + name))) {
			performanceReportOverValue(writer, mathContext, xStart, xEnd, xStep, repeats, functionNames, calculations);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		System.out.println("Finished ");
	}
	
	@SafeVarargs
	private static void performanceReportOverValue(PrintWriter writer, MathContext mathContext, BigDecimal xStart, BigDecimal xEnd, BigDecimal xStep, int repeats, List<String> functionNames, BiFunction<BigDecimal, MathContext, BigDecimal>... calculations) {
		if (functionNames.size() != calculations.length) {
			throw new IllegalArgumentException("Must be same number of functionNames (" + functionNames.size() + ") and functions (" + calculations.length + ")");
		}
		
		// warmup
		for (BigDecimal x = xStart; x.compareTo(xEnd) <= 0; x = x.add(xStep)) {
			for (BiFunction<BigDecimal, MathContext, BigDecimal> calculation : calculations) {
				try {
					calculation.apply(x, MathContext.DECIMAL32);
				}
				catch (ArithmeticException ex) {
					// ignore
				}
			}
		}

		// print headers
		writer.printf("%8s", "x");
		for (int i = 0; i < functionNames.size(); i++) {
			writer.print(",");
			writer.printf("%8s", functionNames.get(i));
		}
		writer.println();

		// print types
		writer.printf("%8s", "number");
		for (int i = 0; i < functionNames.size(); i++) {
			writer.print(",");
			writer.printf("%8s", "number");
		}		
		writer.println();

		// real measurement
		for (BigDecimal x = xStart; x.compareTo(xEnd) <= 0; x = x.add(xStep)) {
			long[] elapsedNanos = new long[calculations.length];

			for (int i = 0; i < calculations.length; i++) {
				BiFunction<BigDecimal, MathContext, BigDecimal> calculation = calculations[i];
				long[] nanos = new long[repeats];

				try {
					for (int j = 0; j < repeats; j++) {
						StopWatch stopWatch = new StopWatch();
						calculation.apply(x, mathContext);
						nanos[j] = stopWatch.getElapsedNanos();
					}
					elapsedNanos[i] = median(nanos);
				}
				catch (ArithmeticException ex) {
					// ignore
				}
			}

			writer.printf("%8.3f", x);
			for (int i = 0; i < elapsedNanos.length; i++) {
				writer.print(",");
				writer.printf("%8d", elapsedNanos[i]);
			}
			writer.println();
		}
	}

	private static long median(long[] values) {
		Arrays.sort(values);
		int halfIndex = values.length / 2;
		if (values.length > 1 && values.length % 2 == 0) {
			return (values[halfIndex] + values[halfIndex + 1]) / 2;
		}
		else {
			return values[halfIndex];
		}
	}

	@SafeVarargs
	private static void performanceReportOverPrecision(BigDecimal value, int precisionStart, int precisionEnd, int precisionStep, BiFunction<BigDecimal, MathContext, BigDecimal>... calculations) {
		// warmup
		for (int i = 0; i < 1000; i++) {
			for (BiFunction<BigDecimal, MathContext, BigDecimal> calculation : calculations) {
				try {
					calculation.apply(value, MathContext.DECIMAL32);
				}
				catch (ArithmeticException ex) {
					// ignore
				}
			}
		}

		// real measurement
		for (int precision = precisionStart; precision < precisionEnd; precision += precisionStep) {
			long[] elapsedMillis = new long[calculations.length];
			MathContext mathContext = new MathContext(precision);

			for (int i = 0; i < calculations.length; i++) {
				BiFunction<BigDecimal, MathContext, BigDecimal> calculation = calculations[i];

				StopWatch stopWatch = new StopWatch();

				try {
					calculation.apply(value, mathContext);
					elapsedMillis[i] = stopWatch.getElapsedMillis();
				}
				catch (ArithmeticException ex) {
					// ignore
				}
			}

			System.out.printf("%9d", precision);
			for (int i = 0; i < elapsedMillis.length; i++) {
				System.out.print(",");
				System.out.printf("%6d", elapsedMillis[i]);
			}
			System.out.println();
		}
	}
}
