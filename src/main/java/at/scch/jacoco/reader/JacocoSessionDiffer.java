package at.scch.jacoco.reader;

public class JacocoSessionDiffer {

    public static JacocoSessionDiff computeDiff(JacocoSession a, JacocoSession b){
        JacocoSession onlyA = new JacocoSession("onlyA " + System.currentTimeMillis());
        JacocoSession common = new JacocoSession("common " + System.currentTimeMillis());
        JacocoSession onlyB = new JacocoSession("onlyB " + System.currentTimeMillis());

        onlyA.add(a);
        onlyA.remove(b);

        common.add(a);
        common.retain(b);

        onlyB.add(b);
        onlyB.remove(a);

        return new JacocoSessionDiff(onlyA, common, onlyB);
    }
}
