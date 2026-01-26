package org.ml.nodeselection

/**
 * GitAnalyzer - Extract metrics from git repository
 * 
 * Analyzes the current git state to extract:
 * - Number of files changed
 * - Lines added/deleted
 * - Dependencies modified
 * - Branch information
 */
class GitAnalyzer implements Serializable {
    
    def steps
    
    GitAnalyzer(steps) {
        this.steps = steps
    }
    
    /**
     * Analyze git changes and return metrics
     */
    Map analyze() {
        def metrics = [
            filesChanged: 0,
            linesAdded: 0,
            linesDeleted: 0,
            depsChanged: 0,
            branch: 'unknown'
        ]
        
        // Get branch name
        try {
            metrics.branch = steps.env.BRANCH_NAME ?: 
                (steps.isUnix() ? 
                    steps.sh(script: 'git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown"', returnStdout: true).trim() :
                    steps.bat(script: '@git rev-parse --abbrev-ref HEAD 2>nul || echo unknown', returnStdout: true).trim())
        } catch (Exception e) {
            metrics.branch = 'unknown'
        }
        
        // Files changed
        try {
            def filesOutput
            if (steps.isUnix()) {
                filesOutput = steps.sh(
                    script: 'git diff --name-only HEAD~1 2>/dev/null | wc -l || echo 0',
                    returnStdout: true
                ).trim()
            } else {
                filesOutput = steps.bat(
                    script: '@git diff --name-only HEAD~1 2>nul | find /c /v ""',
                    returnStdout: true
                ).trim()
            }
            metrics.filesChanged = filesOutput.isInteger() ? filesOutput.toInteger() : 0
        } catch (Exception e) {
            metrics.filesChanged = 0
        }
        
        // Lines added/deleted
        try {
            def diffStat
            if (steps.isUnix()) {
                diffStat = steps.sh(
                    script: 'git diff --shortstat HEAD~1 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
            } else {
                diffStat = steps.bat(
                    script: '@git diff --shortstat HEAD~1 2>nul',
                    returnStdout: true
                ).trim()
            }
            
            // Parse insertions
            def insertMatcher = diffStat =~ /(\d+) insertion/
            if (insertMatcher) {
                metrics.linesAdded = insertMatcher[0][1].toInteger()
            }
            
            // Parse deletions
            def deleteMatcher = diffStat =~ /(\d+) deletion/
            if (deleteMatcher) {
                metrics.linesDeleted = deleteMatcher[0][1].toInteger()
            }
        } catch (Exception e) {
            // Keep defaults
        }
        
        // Check for dependency file changes
        metrics.depsChanged = checkDependencyChanges()
        
        return metrics
    }
    
    /**
     * Check if dependency files were modified
     */
    private int checkDependencyChanges() {
        def depFiles = [
            'build.gradle', 'build.gradle.kts',
            'pom.xml',
            'package.json', 'package-lock.json',
            'requirements.txt', 'Pipfile',
            'Gemfile', 'Gemfile.lock',
            'go.mod', 'go.sum',
            'Cargo.toml'
        ]
        
        def changed = 0
        
        try {
            def changedFiles
            if (steps.isUnix()) {
                changedFiles = steps.sh(
                    script: 'git diff --name-only HEAD~1 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
            } else {
                changedFiles = steps.bat(
                    script: '@git diff --name-only HEAD~1 2>nul',
                    returnStdout: true
                ).trim()
            }
            
            depFiles.each { depFile ->
                if (changedFiles.contains(depFile)) {
                    changed++
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        return changed
    }
}
