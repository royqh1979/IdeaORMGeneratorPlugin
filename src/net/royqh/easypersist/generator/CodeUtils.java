package net.royqh.easypersist.generator;

import net.royqh.easypersist.model.CollectionProperty;
import net.royqh.easypersist.model.Entity;
import net.royqh.easypersist.model.Property;
import net.royqh.easypersist.model.SingleProperty;

/**
 * Created by Roy on 2016/2/18.
 */
public class CodeUtils {

    public static String getEntityMapName(Entity entity) {
        return entity.getName()+"Map";
    }


    public static String getRowCallbackHandlerClassName(Entity entity) {
        return entity.getClassInfo().getName()+"RowCallbackHandler";
    }

    public static String getPropertyVarName(Entity entity, Property property) {
        return entity.getName()+"_"+property.getName();
    }

    public static void appendEntityDeclareStatement(StringBuilder content, Entity entity) {
        content.append(
                String.format("%s %s;\n",
                        entity.getClassInfo().getName(),
                        entity.getName()
                        ));

    }

    public static String getIdVarName(Entity entity) {
        SingleProperty idProperty=entity.getIdProperty();
        return getPropertyVarName(entity, idProperty);
    }

    public static String getCollectionImplementType(CollectionProperty property) {
        String type=property.getCollectionType();
        switch(type) {
            case "java.util.List":
            case "java.util.Collection":
                return "ArrayList<>";
            case "java.util.Set":
                return "HashSet<>";
        }
        return type;
    }


}
