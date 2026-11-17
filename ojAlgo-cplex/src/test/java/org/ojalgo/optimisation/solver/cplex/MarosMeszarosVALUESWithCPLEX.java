package org.ojalgo.optimisation.solver.cplex;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * VALUES, from the Maros-Meszaros collection of convex QPs, as CPLEX sees it - read with CPLEX's own MPS
 * reader, not via SolverCPLEX. Only JUnit and CPLEX are used, nothing from ojAlgo, so it runs with the Java
 * version of this module whatever the ojAlgo build targets.
 * <p>
 * As distributed (identical in the collection's original QPDATA3.ZIP and in CUTEr's SIF version) [Q] is not
 * positive semidefinite: the smallest eigenvalue is about -1.3E-5, with max|Q| = 1. [Q] is two banded blocks
 * of a sampled Gaussian kernel, exp(-d<sup>2</sup>/37.5), cut at |d| = 20 and rounded to 6 decimals. The
 * exact kernel is positive definite, the cut and the rounding each make it indefinite.
 * <p>
 * Solved as a non-convex QP (OptimalityTarget OptimalGlobal) CPLEX finds a solution with the listed optimal
 * value, -1.3966211, but in 60 s the global search only bounds the optimum from below by -8.95.
 */
public class MarosMeszarosVALUESWithCPLEX {

    private static IloCplex newCplex(final Path mps, final int optimalityTarget) throws IloException {

        IloCplex cplex = new IloCplex();
        cplex.setOut(OutputStream.nullOutputStream());
        cplex.setWarning(OutputStream.nullOutputStream());
        cplex.setParam(IloCplex.Param.OptimalityTarget, optimalityTarget);
        cplex.importModel(mps.toString());

        return cplex;
    }

    /**
     * CPLEX reads MPS in free format, where the bounds need a bound set name. In the file it's left blank
     * (fixed format), and then CPLEX takes the column name to be the bound set name.
     */
    private static Path writeModel() throws Exception {

        List<String> lines;
        try (InputStream input = MarosMeszarosVALUESWithCPLEX.class.getResourceAsStream("/optimisation/marosmeszaros/VALUES.SIF")) {
            lines = new String(input.readAllBytes(), StandardCharsets.US_ASCII).lines().collect(Collectors.toList());
        }

        List<String> named = new ArrayList<>(lines.size());
        boolean bounds = false;
        for (String line : lines) {
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))) {
                bounds = line.startsWith("BOUNDS");
                named.add(line);
            } else if (bounds && !line.isBlank()) {
                String[] fields = line.trim().split("\\s+");
                boolean withValue = !Set.of("FR", "MI", "PL", "BV").contains(fields[0]);
                if (fields.length == (withValue ? 3 : 2)) {
                    named.add(" " + fields[0] + " BND " + String.join(" ", Arrays.copyOfRange(fields, 1, fields.length)));
                } else {
                    named.add(line);
                }
            } else {
                named.add(line);
            }
        }

        Path mps = Files.createTempFile("VALUES", ".mps");
        Files.write(mps, named, StandardCharsets.US_ASCII);

        return mps;
    }

    /**
     * By default CPLEX only solves convex QPs, and checks that [Q] is positive semidefinite - it refuses VALUES.
     */
    @Test
    public void testConvexOnly() throws Exception {

        Path mps = MarosMeszarosVALUESWithCPLEX.writeModel();
        IloCplex cplex = MarosMeszarosVALUESWithCPLEX.newCplex(mps, IloCplex.OptimalityTarget.Auto);

        try {
            IloException refused = Assertions.assertThrows(IloException.class, cplex::solve);
            Assertions.assertTrue(refused.getMessage().contains("convex"), refused.getMessage());
        } finally {
            cplex.end();
            Files.deleteIfExists(mps);
        }
    }

}
