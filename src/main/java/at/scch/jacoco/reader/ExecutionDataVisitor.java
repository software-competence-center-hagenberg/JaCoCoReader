package at.scch.jacoco.reader;

import org.jacoco.core.data.*;

import java.util.*;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.ExecutionDataVisitor.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Visitor for parsing JaCoCo coverage file.
 * This visitor separates the coverage data by session.
 */
public class ExecutionDataVisitor implements ISessionInfoVisitor, IExecutionDataVisitor {

    private final Map<String, SessionInfo> sessionInfos = new HashMap<>();

    private final Map<String, ExecutionDataStore> sessions = new LinkedHashMap<>();

    private ExecutionDataStore executionDataStore;
    private ExecutionDataStore merged = new ExecutionDataStore();

    @Override
    public void visitSessionInfo(SessionInfo info) {
        String sessionId = info.getId();
        executionDataStore = sessions.computeIfAbsent(sessionId, id -> new ExecutionDataStore());
        sessionInfos.computeIfAbsent(sessionId, id -> info);
    }

    @Override
    public void visitClassExecution(ExecutionData data) {
        executionDataStore.put(data);
        merged.put(defensiveCopy(data));
    }

    public Map<String, ExecutionDataStore> getSessions() {
        return sessions;
    }

    public SessionInfo getSession(String sessionId){
        return sessionInfos.get(sessionId);
    }

    public ExecutionDataStore getMerged() {
        return merged;
    }

    private static ExecutionData defensiveCopy(ExecutionData data) {
        boolean[] src = data.getProbes();
        boolean[] dest = new boolean[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);
        return new ExecutionData(data.getId(), data.getName(), dest);
    }

    public List<SessionInfo> getSessionInfos() {
        return new LinkedList<>(sessionInfos.values());
    }
}