package org.ml.nodeselection

/**
 * LabelMapper - Maps predicted memory to AWS labels
 * 
 * Your AWS EC2 Configuration:
 * ┌──────────────┬─────────┬─────────────────────┐
 * │ Instance     │ Memory  │ Labels              │
 * ├──────────────┼─────────┼─────────────────────┤
 * │ T3a Small    │ 1 GB    │ lightweight         │
 * │ T3a Small    │ 2 GB    │ linux, executor     │
 * │ T3a Large    │ 8 GB    │ build, deploy       │
 * │ T3a X Large  │ 16 GB   │ security, test      │
 * │ T3a 2X Large │ 32 GB   │ heavytest           │
 * └──────────────┴─────────┴─────────────────────┘
 */
class LabelMapper implements Serializable {
    
    // AWS instance configurations
    static final Map<String, Map> INSTANCES = [
        'lightweight': [memory: 1, instance: 'T3a Small', cost: 0.0047, executors: 1],
        'executor':    [memory: 2, instance: 'T3a Small', cost: 0.0094, executors: 3],
        'build':       [memory: 8, instance: 'T3a Large', cost: 0.0376, executors: 2],
        'test':        [memory: 16, instance: 'T3a X Large', cost: 0.0752, executors: 1],
        'heavytest':   [memory: 32, instance: 'T3a 2X Large', cost: 0.1504, executors: 1]
    ]
    
    /**
     * Get the appropriate Jenkins label based on predicted memory
     */
    String getLabel(double predictedMemoryGb) {
        // Add 20% buffer for safety
        double requiredMemory = predictedMemoryGb * 1.2
        
        if (requiredMemory <= 1.0) {
            return 'lightweight'
        } else if (requiredMemory <= 2.0) {
            return 'executor'
        } else if (requiredMemory <= 8.0) {
            return 'build'
        } else if (requiredMemory <= 16.0) {
            return 'test'
        } else {
            return 'heavytest'
        }
    }
    
    /**
     * Get AWS instance type for display
     */
    String getInstanceType(double predictedMemoryGb) {
        String label = getLabel(predictedMemoryGb)
        return INSTANCES[label]?.instance ?: 'Unknown'
    }
    
    /**
     * Get the memory available for a label
     */
    int getMemoryForLabel(String label) {
        return INSTANCES[label]?.memory ?: 8
    }
    
    /**
     * Get hourly cost for the selected instance
     */
    double getHourlyCost(String label) {
        return INSTANCES[label]?.cost ?: 0.0
    }
    
    /**
     * Get all available labels
     */
    List<String> getAllLabels() {
        return INSTANCES.keySet().toList()
    }
    
    /**
     * Calculate potential savings compared to always using largest
     */
    Map calculateSavings(String selectedLabel, double buildTimeHours) {
        double selectedCost = INSTANCES[selectedLabel]?.cost ?: 0.0
        double maxCost = INSTANCES['heavytest']?.cost ?: 0.0
        
        double actualCost = selectedCost * buildTimeHours
        double maxBasedCost = maxCost * buildTimeHours
        double savings = maxBasedCost - actualCost
        
        return [
            selectedCost: actualCost.round(4),
            worstCaseCost: maxBasedCost.round(4),
            savings: savings.round(4),
            savingsPercent: maxBasedCost > 0 ? (savings / maxBasedCost * 100).round(1) : 0
        ]
    }
    
    /**
     * Get a summary of the label configuration
     */
    String getLabelSummary(String label) {
        def config = INSTANCES[label]
        if (!config) return "Unknown label: ${label}"
        
        return "${label} → ${config.instance} (${config.memory}GB, \$${config.cost}/hr)"
    }
}
