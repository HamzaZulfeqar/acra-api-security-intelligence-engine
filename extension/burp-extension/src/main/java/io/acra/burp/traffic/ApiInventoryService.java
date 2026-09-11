package io.acra.burp.traffic;
import io.acra.core.domain.endpoint.*;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.inventory.EndpointInventory;
import java.util.*;
public final class ApiInventoryService {
    private final EndpointInventory inventory;
    public ApiInventoryService(EndpointInventory inventory){this.inventory=Objects.requireNonNull(inventory);}
    public List<ApiEndpointRecord> snapshot(){
        return inventory.snapshot().stream().map(a->{ AuthenticationType auth=a.authenticationTypes().size()==1?a.authenticationTypes().iterator().next():AuthenticationType.UNKNOWN;
            return new ApiEndpointRecord(a.endpoint(),a.endpoint().version(),"UNKNOWN",auth,"",a.firstObserved(),a.lastObserved(),DocumentationStatus.UNKNOWN,RiskTier.UNKNOWN); }).toList();
    }
}
