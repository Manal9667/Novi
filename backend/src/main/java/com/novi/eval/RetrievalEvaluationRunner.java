package com.novi.eval;

/**
 * Reproducible, self-contained evaluation workflow. Generates a deterministic
 * labeled dataset (fixed seed), runs the embedding-representation experiment,
 * and prints a metrics comparison plus the best strategy. Runs without Spring,
 * a database, or any API key, so results are fully reproducible.
 *
 * <p>Run it with:
 * <pre>
 *   cd backend
 *   mvn -q compile
 *   java -cp target/classes com.novi.eval.RetrievalEvaluationRunner
 * </pre>
 * The same output is asserted by {@code RetrievalEvaluationHarnessTest} so the
 * workflow is exercised in CI.
 */
public final class RetrievalEvaluationRunner {

    // Fixed experiment parameters -> reproducible dataset and metrics.
    public static final long SEED = 42L;
    public static final int NUM_TOPICS = 8;
    public static final int DOCS_PER_TOPIC = 8;
    public static final int QUERIES_PER_TOPIC = 4;
    public static final int DIM = 64;
    public static final double NOISE = 0.15;
    public static final int K = 10;

    private RetrievalEvaluationRunner() {}

    public static EmbeddingRepresentationExperiment.ExperimentOutcome runDefaultExperiment() {
        SyntheticEvaluationData.Dataset data =
                SyntheticEvaluationData.generate(SEED, NUM_TOPICS, DOCS_PER_TOPIC, QUERIES_PER_TOPIC, DIM, NOISE);
        return new EmbeddingRepresentationExperiment().run(data.corpus(), data.queries(), K);
    }

    public static void main(String[] args) {
        EmbeddingRepresentationExperiment.ExperimentOutcome outcome = runDefaultExperiment();

        System.out.println("Novi retrieval-quality evaluation (seed=" + SEED + ", dim=" + DIM
                + ", topics=" + NUM_TOPICS + ", k=" + K + ")");
        System.out.println("-------------------------------------------------------------");
        for (EmbeddingRepresentationExperiment.StrategyResult r : outcome.results()) {
            System.out.println(r.report().toTableRow(r.strategy().name()));
        }
        System.out.println("-------------------------------------------------------------");
        System.out.println("Best strategy by nDCG@" + K + ": " + outcome.best().strategy().name()
                + String.format(" (nDCG=%.3f)", outcome.best().report().meanNdcg()));
    }
}
