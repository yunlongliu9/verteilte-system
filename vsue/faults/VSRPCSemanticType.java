package vsue.faults;

public enum VSRPCSemanticType {
    LAST_OF_MANY,
    AT_MOST_ONCE;

    public VSRPCSemanticHandler createHandler(){
        switch(this) {
            case LAST_OF_MANY:
                return new VSRPCSemanticLOMHandler();
            case AT_MOST_ONCE:
                return new VSRPCSemanticAMOHandler();
            default:
                throw new RuntimeException(
                    "Unknown semantic"
                );
        }
    }
}
