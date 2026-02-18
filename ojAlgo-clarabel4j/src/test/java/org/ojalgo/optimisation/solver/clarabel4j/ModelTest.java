package org.ojalgo.optimisation.solver.clarabel4j;

import static com.ustermetrics.clarabel4j.DirectSolveMethod.*;
import static com.ustermetrics.clarabel4j.Status.SOLVED;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.ustermetrics.clarabel4j.*;

class ModelTest {

    private static final double TOLERANCE = 1e-8;

    @Test
    void cleanupBeforeOptimizeThrowsException() {
        var exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            try (var model = new Model()) {
                model.cleanup();
            }
        });

        Assertions.assertEquals("model must not be in stage new", exception.getMessage());
    }

    @Test
    void getSolutionBeforeOptimizeThrowsException() {
        var exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            try (var model = new Model()) {
                model.x();
            }
        });

        Assertions.assertEquals("model must be in stage optimized", exception.getMessage());
    }

    @Test
    void optimizeBeforeSetupThrowsException() {
        var exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            try (var model = new Model()) {
                model.optimize();
            }
        });

        Assertions.assertEquals("model must not be in stage new", exception.getMessage());
    }

    @Test
    void setAllParametersDoesNotThrow() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().maxIter(1).timeLimit(1.).verbose(true).maxStepFraction(1.).tolGapAbs(1.).tolGapRel(1.).tolFeas(1.)
                .tolInfeasAbs(1.).tolInfeasRel(1.).tolKtratio(1.).reducedTolGapAbs(1.).reducedTolGapRel(1.).reducedTolFeas(1.).reducedTolInfeasAbs(1.)
                .reducedTolInfeasRel(1.).reducedTolKtratio(1.).equilibrateEnable(true).equilibrateMaxIter(1).equilibrateMinScaling(1.).equilibrateMaxScaling(1.)
                .linesearchBacktrackStep(1.).minSwitchStepLength(1.).minTerminateStepLength(1.).maxThreads(0).directKktSolver(true).directSolveMethod(QDLDL)
                .staticRegularizationEnable(true).staticRegularizationConstant(1.).staticRegularizationProportional(1.).dynamicRegularizationEnable(true)
                .dynamicRegularizationEps(1.).dynamicRegularizationDelta(1.).iterativeRefinementEnable(true).iterativeRefinementReltol(1.)
                .iterativeRefinementAbstol(1.).iterativeRefinementMaxIter(1).iterativeRefinementStopRatio(1.).presolveEnable(true).build();

        Assertions.assertDoesNotThrow(() -> {
            try (var model = new Model()) {
                model.setParameters(parameters);
                model.setup(p);
            }
        });
    }

    @Test
    void setParametersAfterSetupThrowsException() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(false).build();

        var exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            try (var model = new Model()) {
                model.setup(p);
                model.setParameters(parameters);
            }
        });

        Assertions.assertEquals("model must be in stage new", exception.getMessage());
    }

    @Test
    void setupAfterOptimizeThrowsException() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(false).build();

        var exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            try (var model = new Model()) {
                model.setParameters(parameters);
                model.setup(p);
                model.optimize();
                model.setup(p);
            }
        });

        Assertions.assertEquals("model must be in stage new", exception.getMessage());
    }

    @Test
    void solveExponentialConeProgramReturnsExpectedSolution() {
        // Exponential cone program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_expcone.c
        var p = new Matrix(3, 3, new long[] { 0, 0, 0, 0 }, new long[] {}, new double[] {});
        var q = new double[] { -1., 0., 0. };
        var a = new Matrix(5, 3, new long[] { 0, 1, 3, 5 }, new long[] { 0, 1, 3, 2, 4 }, new double[] { -1., -1., 1., -1., 1. });
        var b = new double[] { 0., 0., 0., 1., Math.exp(5.) };
        var cones = List.of(new ExponentialCone(), new ZeroCone(2));
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 4.999999991697934, 1., 148.41315907885752 }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { -1., 4.000066891615788, 0.006737496384128253, 4.0000668915268625, 0.006737496182862412 }, model.z(),
                    TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 4.999999991529423, 1., 148.41315907903652, 0., 0. }, model.s(), TOLERANCE);
        }
    }

    @Test
    void solveExponentialConeProgramWithoutMatrixPReturnsExpectedSolution() {
        // Exponential cone program from the Clarabel examples using model.setup() without matrix P
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_expcone.c
        var q = new double[] { -1., 0., 0. };
        var a = new Matrix(5, 3, new long[] { 0, 1, 3, 5 }, new long[] { 0, 1, 3, 2, 4 }, new double[] { -1., -1., 1., -1., 1. });
        var b = new double[] { 0., 0., 0., 1., Math.exp(5.) };
        var cones = List.of(new ExponentialCone(), new ZeroCone(2));
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 4.999999991697934, 1., 148.41315907885752 }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { -1., 4.000066891615788, 0.006737496384128253, 4.0000668915268625, 0.006737496182862412 }, model.z(),
                    TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 4.999999991529423, 1., 148.41315907903652, 0., 0. }, model.s(), TOLERANCE);
        }
    }

    @Test
    void solveFeasibilityProblemReturnsExpectedSolution() {
        // [[6., 0.],
        //  [0., 4.]]
        var a = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var b = new double[] { 6., 1. };
        final List<Cone> cones = List.of(new ZeroCone(2));
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1., 0.25 }, model.x(), TOLERANCE);
        }
    }

    @Test
    void solveGeneralizedPowerConeProgramReturnsExpectedSolution() {
        // Generalized power cone program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_genpowcone.c
        var p = new Matrix(6, 6, new long[] { 0, 0, 0, 0, 0, 0, 0 }, new long[] {}, new double[] {});
        var q = new double[] { 0., 0., -1., 0., 0., -1. };
        var a = new Matrix(8, 6, new long[] { 0, 2, 4, 5, 7, 9, 10 }, new long[] { 0, 6, 1, 6, 2, 3, 6, 4, 7, 5 },
                new double[] { -1., 1., -1., 2., -1., -1., 3., -1., 1., -1. });
        var b = new double[] { 0., 0., 0., 0., 0., 0., 3., 1. };
        var cones = List.of(new GenPowerCone(new double[] { 0.6, 0.4 }, 1), new GenPowerCone(new double[] { 0.1, 0.9 }, 1), new ZeroCone(1), new ZeroCone(1));
        var parameters = Parameters.builder().verbose(false).maxIter(100).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1.6818105475, 0.5606033992, 1.0837485448, 0.0656608752, 1., 0.7616065300 }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0.3866364127, 0.7732728115, -1., 1.1599092115, 0.6854458898, -1., 0.3866363997, 0.6854458735 },
                    model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 1.6818105584, 0.5606034095, 1.0837485446, 0.0656608832, 1., 0.7616065300, 0., 0. }, model.s(),
                    TOLERANCE);
        }
    }

    @Test
    void solveGeneralizedPowerConeProgramWithoutMatrixPReturnsExpectedSolution() {
        // Generalized power cone program from the Clarabel examples using model.setup() without matrix P
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_genpowcone.c
        var q = new double[] { 0., 0., -1., 0., 0., -1. };
        var a = new Matrix(8, 6, new long[] { 0, 2, 4, 5, 7, 9, 10 }, new long[] { 0, 6, 1, 6, 2, 3, 6, 4, 7, 5 },
                new double[] { -1., 1., -1., 2., -1., -1., 3., -1., 1., -1. });
        var b = new double[] { 0., 0., 0., 0., 0., 0., 3., 1. };
        var cones = List.of(new GenPowerCone(new double[] { 0.6, 0.4 }, 1), new GenPowerCone(new double[] { 0.1, 0.9 }, 1), new ZeroCone(1), new ZeroCone(1));
        var parameters = Parameters.builder().verbose(false).maxIter(100).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1.6818105475, 0.5606033992, 1.0837485448, 0.0656608752, 1., 0.7616065300 }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0.3866364127, 0.7732728115, -1., 1.1599092115, 0.6854458898, -1., 0.3866363997, 0.6854458735 },
                    model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 1.6818105584, 0.5606034095, 1.0837485446, 0.0656608832, 1., 0.7616065300, 0., 0. }, model.s(),
                    TOLERANCE);
        }
    }

    @Test
    void solveLinearProgramReturnsExpectedSolution() {
        // Linear program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_lp.c
        var p = new Matrix(2, 2, new long[] { 0, 0, 0 }, new long[] {}, new double[] {});
        var q = new double[] { 1., -1. };
        var a = new Matrix(4, 2, new long[] { 0, 2, 4 }, new long[] { 0, 2, 1, 3 }, new double[] { 1., -1., 1., -1. });
        var b = new double[] { 1., 1., 1., 1. };
        final List<Cone> cones = List.of(new NonnegativeCone(4));
        var parameters = Parameters.builder().equilibrateEnable(true).equilibrateMaxIter(50).directSolveMethod(QDLDL).maxThreads(1).verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { -1., 1. }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0., 1., 1., 0. }, model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 2., 0., 0., 2. }, model.s(), TOLERANCE);
            Assertions.assertEquals(-2., model.objVal(), TOLERANCE);
            Assertions.assertEquals(-2., model.objValDual(), TOLERANCE);
            Assertions.assertTrue(model.solveTime() > 0.);
            Assertions.assertEquals(6, model.iterations());
            Assertions.assertEquals(0., model.rPrim(), TOLERANCE);
            Assertions.assertEquals(0., model.rDual(), TOLERANCE);
            Assertions.assertEquals(QDLDL, model.directSolveMethod());
            Assertions.assertEquals(1, model.threads());
            Assertions.assertEquals(10, model.nnzA());
            Assertions.assertEquals(4, model.nnzL());
        }
    }

    @Test
    void solveLinearProgramWithoutMatrixPReturnsExpectedSolution() {
        // Linear program from the Clarabel examples using model.setup() without matrix P
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_lp.c
        var q = new double[] { 1., -1. };
        var a = new Matrix(4, 2, new long[] { 0, 2, 4 }, new long[] { 0, 2, 1, 3 }, new double[] { 1., -1., 1., -1. });
        var b = new double[] { 1., 1., 1., 1. };
        final List<Cone> cones = List.of(new NonnegativeCone(4));
        var parameters = Parameters.builder().equilibrateEnable(true).equilibrateMaxIter(50).verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { -1., 1. }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0., 1., 1., 0. }, model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 2., 0., 0., 2. }, model.s(), TOLERANCE);
        }
    }

    @Disabled("Needs Intel CPU, installation of Pardiso from Intel oneAPI Base Toolkit, and 'libmkl_rt.so' must be "
            + "on the system library path (e.g. on 'LD_LIBRARY_PATH' on Linux)")
    @Test
    void solveLinearProgramWithPardisoReturnsExpectedSolution() {
        // Linear program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_lp.c
        var p = new Matrix(2, 2, new long[] { 0, 0, 0 }, new long[] {}, new double[] {});
        var q = new double[] { 1., -1. };
        var a = new Matrix(4, 2, new long[] { 0, 2, 4 }, new long[] { 0, 2, 1, 3 }, new double[] { 1., -1., 1., -1. });
        var b = new double[] { 1., 1., 1., 1. };
        final List<Cone> cones = List.of(new NonnegativeCone(4));
        var pardisoIParm = new int[64];
        pardisoIParm[1] = 0;
        var parameters = Parameters.builder().directSolveMethod(PARDISO_MKL).pardisoIparm(pardisoIParm).pardisoVerbose(false).verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { -1., 1. }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0., 1., 1., 0. }, model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 2., 0., 0., 2. }, model.s(), TOLERANCE);
            Assertions.assertEquals(-2., model.objVal(), TOLERANCE);
            Assertions.assertEquals(-2., model.objValDual(), TOLERANCE);
            Assertions.assertTrue(model.solveTime() > 0.);
            Assertions.assertEquals(6, model.iterations());
            Assertions.assertEquals(0., model.rPrim(), TOLERANCE);
            Assertions.assertEquals(0., model.rDual(), TOLERANCE);
            Assertions.assertEquals(PARDISO_MKL, model.directSolveMethod());
            Assertions.assertTrue(model.threads() >= 1);
            Assertions.assertEquals(10, model.nnzA());
            Assertions.assertEquals(7, model.nnzL());
        }
    }

    @Test
    void solvePowerConeProgramReturnsExpectedSolution() {
        // Power cone program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_powcone.c
        var p = new Matrix(6, 6, new long[] { 0, 0, 0, 0, 0, 0, 0 }, new long[] {}, new double[] {});
        var q = new double[] { 0., 0., -1., 0., 0., -1. };
        var a = new Matrix(8, 6, new long[] { 0, 2, 4, 5, 7, 9, 10 }, new long[] { 0, 6, 1, 6, 2, 3, 6, 4, 7, 5 },
                new double[] { -1., 1., -1., 2., -1., -1., 3., -1., 1., -1. });
        var b = new double[] { 0., 0., 0., 0., 0., 0., 3., 1. };
        var cones = List.of(new PowerCone(0.6), new PowerCone(0.1), new ZeroCone(1), new ZeroCone(1));
        var parameters = Parameters.builder().verbose(false).maxIter(100).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(
                    new double[] { 1.6817569543698885, 0.5606519701613502, 1.0837653988697291, 0.06564635991505167, 1., 0.7615896968982815 }, model.x(),
                    TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0.3866364120, 0.7732728161, -1., 1.1599092208, 0.6854458909, -1., 0.3866364045, 0.6854458815 },
                    model.z(), TOLERANCE);
            Assertions.assertArrayEquals(
                    new double[] { 1.6817569643718597, 0.5606519795173677, 1.083765398869734, 0.0656463671023277, 1., 0.7615896968983085, 0., 0. }, model.s(),
                    TOLERANCE);
        }
    }

    @Test
    void solvePowerConeProgramWithoutMatrixPReturnsExpectedSolution() {
        // Power cone program from the Clarabel examples using model.setup() without matrix P
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_powcone.c
        var q = new double[] { 0., 0., -1., 0., 0., -1. };
        var a = new Matrix(8, 6, new long[] { 0, 2, 4, 5, 7, 9, 10 }, new long[] { 0, 6, 1, 6, 2, 3, 6, 4, 7, 5 },
                new double[] { -1., 1., -1., 2., -1., -1., 3., -1., 1., -1. });
        var b = new double[] { 0., 0., 0., 0., 0., 0., 3., 1. };
        var cones = List.of(new PowerCone(0.6), new PowerCone(0.1), new ZeroCone(1), new ZeroCone(1));
        var parameters = Parameters.builder().verbose(false).maxIter(100).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(
                    new double[] { 1.6817569543698885, 0.5606519701613502, 1.0837653988697291, 0.06564635991505167, 1., 0.7615896968982815 }, model.x(),
                    TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0.3866364120, 0.7732728161, -1., 1.1599092208, 0.6854458909, -1., 0.3866364045, 0.6854458815 },
                    model.z(), TOLERANCE);
            Assertions.assertArrayEquals(
                    new double[] { 1.6817569643718597, 0.5606519795173677, 1.083765398869734, 0.0656463671023277, 1., 0.7615896968983085, 0., 0. }, model.s(),
                    TOLERANCE);
        }
    }

    @Test
    void solveProblemTwiceWithCleanupInBetweenReturnsExpectedSolution() {
        // [[6., 0.],
        //  [0., 4.]]
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var q = new double[] { -1., -4. };
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q);
            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1. / 6., 1. }, model.x(), TOLERANCE);

            model.cleanup();

            q = new double[] { -2., -4. };
            model.setup(p, q);
            status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1. / 3., 1. }, model.x(), TOLERANCE);
        }
    }

    @Test
    void solveProblemWithArenaReturnsSolved() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(false).build();

        try (var arena = Arena.ofConfined(); var model = new Model(arena)) {
            model.setParameters(parameters);
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
        }
    }

    @Test
    void solveProblemWithFileOutputReturnsExpected() throws IOException {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(true).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            var tempFile = File.createTempFile("clarabel4j-output-", ".txt");
            model.setOutput(new FileOutput(tempFile.getAbsolutePath()));
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            var tempFilePath = tempFile.toPath();
            var stringOutput = Files.readString(tempFilePath);
            Assertions.assertNotNull(stringOutput);
            Assertions.assertTrue(stringOutput.contains("Clarabel.rs"));
            Files.delete(tempFilePath);
        }
    }

    @Test
    void solveProblemWithStdOutOutputReturnsSolved() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(true).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setOutput(new StdOutOutput());
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
        }
    }

    @Test
    void solveProblemWithStringOutputReturnsExpected() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(true).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setOutput(new StringOutput());
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertNotNull(model.getStringOutput());
            Assertions.assertTrue(model.getStringOutput().contains("Clarabel.rs"));
        }
    }

    @Test
    void solveProblemWithVerboseParameterTrueReturnsSolved() {
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(true).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
        }
    }

    @Test
    void solveQuadraticProgramReturnsExpectedSolution() {
        // Quadratic program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_qp.c
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var q = new double[] { -1., -4. };
        var a = new Matrix(5, 2, new long[] { 0, 3, 6 }, new long[] { 0, 1, 3, 0, 2, 4 }, new double[] { 1., 1., -1., -2., 1., -1. });
        var b = new double[] { 0., 1., 1., 1., 1. };
        var cones = List.of(new ZeroCone(1), new NonnegativeCone(4));
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 0.4285714282, 0.2142857141 }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { -1.5714285714, 0., 0., 0., 0. }, model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 0., 0.5714285718, 0.7857142859, 1.4285714282, 1.2142857141 }, model.s(), TOLERANCE);
        }
    }

    @Test
    void solveSecondOrderConeProgramReturnsExpectedSolution() {
        // Second-order cone program from the Clarabel examples
        // https://github.com/oxfordcontrol/Clarabel.cpp/blob/main/examples/c/example_socp.c
        var p = new Matrix(2, 2, new long[] { 0, 0, 1 }, new long[] { 1 }, new double[] { 2. });
        var a = new Matrix(3, 2, new long[] { 0, 1, 2 }, new long[] { 1, 2 }, new double[] { -2., -1. });
        var b = new double[] { 1., -2., -2. };
        final List<Cone> cones = List.of(new SecondOrderCone(3));
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, a, b, cones);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1., 1. }, model.x(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 2., 0., 2. }, model.z(), TOLERANCE);
            Assertions.assertArrayEquals(new double[] { 1., 0., -1. }, model.s(), TOLERANCE);
        }
    }

    @Test
    void solveUnconstrainedPureQuadraticProgramReturnsExpectedSolution() {
        // [[6., 0.],
        //  [0., 4.]]
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 0., 0. }, model.x(), TOLERANCE);
        }
    }

    @Test
    void solveUnconstrainedQuadraticProgramReturnsExpectedSolution() {
        // [[6., 0.],
        //  [0., 4.]]
        var p = new Matrix(2, 2, new long[] { 0, 1, 2 }, new long[] { 0, 1 }, new double[] { 6., 4. });
        var q = new double[] { -1., -4. };
        var parameters = Parameters.builder().verbose(false).build();

        try (var model = new Model()) {
            model.setParameters(parameters);
            model.setup(p, q);

            var status = model.optimize();

            Assertions.assertEquals(SOLVED, status);
            Assertions.assertArrayEquals(new double[] { 1. / 6., 1. }, model.x(), TOLERANCE);
        }
    }

}