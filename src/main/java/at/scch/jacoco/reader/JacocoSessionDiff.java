package at.scch.jacoco.reader;

public class JacocoSessionDiff {
    private final JacocoSession onlyA;
    private final JacocoSession common;
    private final JacocoSession onlyB;

    public JacocoSessionDiff(JacocoSession onlyA, JacocoSession common, JacocoSession onlyB) {
        this.onlyA = onlyA;
        this.common = common;
        this.onlyB = onlyB;
    }

    public JacocoSession getOnlyA() {
        return onlyA;
    }

    public JacocoSession getCommon() {
        return common;
    }

    public JacocoSession getOnlyB() {
        return onlyB;
    }

    public boolean containsDifference(){
        return onlyA.getNumberOfCoveredMethods() > 0 || onlyB.getNumberOfCoveredMethods() > 0;
    }
}
