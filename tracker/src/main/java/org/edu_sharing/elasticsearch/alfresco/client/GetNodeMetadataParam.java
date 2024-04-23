package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.List;

public class GetNodeMetadataParam {
    List<Long> nodeIds = new ArrayList<>();


    private boolean includeProperties = true;
    private boolean includeAspects = true;
    private boolean includeType = true;
    private boolean includeAclId = true;
    private boolean includeOwner = true;
    private boolean includePaths = true;
    private boolean includeParentAssociations = true;
    private boolean includeChildAssociations = true;
    private boolean includeNodeRef = true;
    private boolean includeChildIds = true;
    private boolean includeTxnId = true;

    public boolean getIncludeChildAssociations()
    {
        return includeChildAssociations;
    }
    public void setIncludeChildAssociations(boolean includeChildAssociations)
    {
        this.includeChildAssociations = includeChildAssociations;
    }
    public boolean getIncludeNodeRef()
    {
        return includeNodeRef;
    }
    public void setIncludeNodeRef(boolean includeNodeRef)
    {
        this.includeNodeRef = includeNodeRef;
    }
    public boolean getIncludeParentAssociations()
    {
        return includeParentAssociations;
    }
    public void setIncludeParentAssociations(boolean includeParentAssociations)
    {
        this.includeParentAssociations = includeParentAssociations;
    }
    public boolean getIncludeProperties()
    {
        return includeProperties;
    }
    public void setIncludeProperties(boolean includeProperties)
    {
        this.includeProperties = includeProperties;
    }
    public boolean getIncludeAspects()
    {
        return includeAspects;
    }
    public void setIncludeAspects(boolean includeAspects)
    {
        this.includeAspects = includeAspects;
    }
    public boolean getIncludeType()
    {
        return includeType;
    }
    public void setIncludeType(boolean includeType)
    {
        this.includeType = includeType;
    }
    public boolean getIncludeAclId()
    {
        return includeAclId;
    }
    public void setIncludeAclId(boolean includeAclId)
    {
        this.includeAclId = includeAclId;
    }
    public boolean getIncludeOwner()
    {
        return includeOwner;
    }
    public void setIncludeOwner(boolean includeOwner)
    {
        this.includeOwner = includeOwner;
    }
    public boolean getIncludePaths()
    {
        return includePaths;
    }
    public void setIncludePaths(boolean includePaths)
    {
        this.includePaths = includePaths;
    }
    public boolean getIncludeChildIds()
    {
        return includeChildIds;
    }
    public void setIncludeChildIds(boolean includeChildIds)
    {
        this.includeChildIds = includeChildIds;
    }
    public boolean getIncludeTxnId()
    {
        return includeTxnId;
    }
    public void setIncludeTxnId(boolean includeTxnId)
    {
        this.includeTxnId = includeTxnId;
    }


    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds) {
        this.nodeIds = nodeIds;
    }
}
