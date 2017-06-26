package net.royqh.easypersist.generator;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import net.royqh.easypersist.model.*;
import net.royqh.easypersist.model.jpa.Constants;
import net.royqh.easypersist.utils.TypeUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Roy on 2017/6/24.
 */
public class ControllerGenerator {
    public static void generateController(PsiFileFactory psiFileFactory, JavaPsiFacade facade, CodeStyleManager codeStyleManager, Entity entity, PsiDirectory psiOutputDir) {
        String controllerClassName = CodeUtils.getControllerName(entity);
        String fileName = controllerClassName + ".java";

        PsiFile oldFile = psiOutputDir.findFile(fileName);
        //We Only Create compositor when it is not existed;
        if (oldFile != null) {
            oldFile.delete();
        }
        PsiFile psiFile = generateControllerFile(entity, null, psiFileFactory);
        psiFile = (PsiFile) codeStyleManager.reformat(psiFile);
        psiOutputDir.add(psiFile);
    }

    private static PsiFile generateControllerFile(Entity entity, PsiPackage targetPackage, PsiFileFactory psiFileFactory) {
        String controllerClassName = CodeUtils.getControllerName(entity);
        String serviceClassName=CodeUtils.getServiceName(entity);
        String serviceName=entity.getName()+"Service";
        String persistorName=CodeUtils.getPersistorCompositorName(entity);
        StringBuilder content = new StringBuilder();
        if (targetPackage != null) {
            content.append("package " + targetPackage.getQualifiedName() + ";\n");
        } else {
            content.append("package dummy;\n");
        }

        /*-- */
        content.append("import ");
        content.append(entity.getClassInfo().getQualifiedName());
        content.append(";\n");
        content.append("import cn.edu.bjfu.smartforestry.view.ProcessingResultType;\n");
        content.append("import cn.edu.bjfu.smartforestry.view.utils.Result;\n");
        content.append("import cn.edu.bjfu.smartforestry.view.utils.ResultWithEntity;\n");
        content.append("import com.qui.base.Grid;\n");
        content.append("import com.qui.base.Pager;\n");
        content.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        content.append("import org.springframework.stereotype.Controller;\n");
        content.append("import org.springframework.web.bind.annotation.RequestMapping;\n");
        content.append("import org.springframework.web.bind.annotation.RequestMethod;\n");
        content.append("import org.springframework.web.bind.annotation.ResponseBody;\n");
        content.append("import net.royqh.lang.DateTools;\n");


        content.append("import java.util.List;\n");

        Set<String> types = new HashSet<>();
        for (Property property : entity.getProperties()) {
            PropertyType propertyType = property.getPropertyType();
            switch (propertyType) {
                case Column:
                    types.add(TypeUtils.cleanForImport(property.getType()));
                    if (property instanceof ReferenceSingleProperty) {
                        ReferenceSingleProperty referenceSingleProperty=(ReferenceSingleProperty)property;
                        types.add(referenceSingleProperty.getRefEntityFullClassName());
                    }
                    break;
                /*
                case OneToMany:
                    types.add(((OneToManyCollectionProperty) property)
                            .getOneToMany().getTargetEntity());
                    break;
                case ManyToMany:
                    types.add(((ManyToManyCollectionProperty) property)
                            .getManyToMany().getTargetEntity());
                    break;
                case OneToOne:
                    types.add(((OneToOneProperty) property)
                            .getOneToOne().getTargetEntity());
                    break;
                case ManyToOne:
                    types.add(((ManyToOneProperty) property)
                            .getManyToOne().getTargetEntity());
                    break;
                case ElementCollection:
                    types.add(((ElementCollectionProperty) property)
                            .getElementCollection().getTargetClassName());
                    break;
                    */
            }
        }
        for (MapRelationInfo relationInfo : entity.getMapRelationInfos()) {
            types.add(relationInfo.getMappingEntityFullClassName());
            Entity mappingEntity = entity.getMappingRepository().findEntityByClass(relationInfo.getMappingEntityFullClassName());
            if (mappingEntity==null) {
                throw new RuntimeException("Not found entity definition for class "+relationInfo.getMappingEntityFullClassName());
            }
            /* 如果单独为某一个Entity生成Persistor, 这时我们不知道Persistor应该放在哪个包里 */
            if (mappingEntity.getPersistorPackageName()!=null) {
                types.add(mappingEntity.getPersistorPackageName().replaceAll(System.lineSeparator(), ".")
                        + "." + CodeUtils.getPersistorName(mappingEntity));
            }
        }
        types.removeAll(Constants.PRIMITIVE_TYPES);
        types.removeAll(Constants.BASIC_TYPES);
        for (String type : types) {
            content.append("import ");
            content.append(type);
            content.append(";\n");
        }

        content.append("@Controller\n");
        content.append("@RequestMapping(\"codes/");
        content.append(entity.getName());
        content.append("\")\n");
        content.append("public class ");
        content.append(controllerClassName);
        content.append(" {\n");

        content.append("@Autowired\n");
        content.append("private ");
        content.append(serviceClassName);
        content.append(" ");
        content.append(serviceName);
        content.append(";\n");

        /* main method */
        content.append("@RequestMapping(value = \"/main\",method = RequestMethod.GET)\n");
        content.append("public String main(){\n");
        content.append("return \"");
        content.append(entity.getName());
        content.append("\";");
        content.append("}\n");

        /* list method */
        content.append("@RequestMapping(value = \"/list\",method = RequestMethod.POST,\n");
        content.append("produces = \"application/json\")\n");
        content.append("@ResponseBody\n");
        content.append(String.format("public Grid<%s> list() {\n",
                entity.getClassInfo().getName()));
        content.append(String.format("List<%s> list=%s.listAll();\n",
                entity.getClassInfo().getName(),
                serviceName));
        content.append("Pager pager = new Pager(1000, 1);\n");
        content.append("pager.setTotalRows(list.size());\n");
        content.append(String.format("Grid<%s> result = new Grid<>(pager, list, null, null);\n",
                entity.getClassInfo().getName()));
        content.append("return result;\n");
        content.append("}\n");

        /* create method */
        content.append("@RequestMapping(value = \"/create\",method = RequestMethod.POST,\n");
        content.append("produces = \"application/json\")\n");
        content.append("@ResponseBody\n");
        content.append("public ResultWithEntity<");
        content.append(entity.getClassInfo().getName());
        content.append("> create(");
        generateEntityParamList(content,entity,false);
        content.append(") {\n");
        content.append(String.format("%s %s=new %s();\n",
                entity.getClassInfo().getName(),
                entity.getName(),
                entity.getClassInfo().getName()));
        generateEntityPropertySettings(content,entity,false);
        content.append(String.format("%s.create(%s);\n",
                serviceName,
                entity.getName()));
        content.append(String.format("return new ResultWithEntity<>(ProcessingResultType.Success,%s);\n",
                entity.getName()
                ));
        content.append("}\n");


        /* update method */
        content.append("@RequestMapping(value = \"/update\",method = RequestMethod.POST,\n");
        content.append("produces = \"application/json\")\n");
        content.append("@ResponseBody\n");
        content.append(String.format("public ResultWithEntity<%s> update(",
                entity.getClassInfo().getName()));
        generateEntityParamList(content,entity,true);
        content.append(") {\n");
        content.append(String.format("%s %s=%s.retrieve(%s);\n",
                entity.getClassInfo().getName(),
                entity.getName(),
                serviceName,
                entity.getIdProperty().getName()));
        content.append(String.format("if (%s==null)      {\n",
                entity.getName()));
        content.append(String.format("return new ResultWithEntity<>(ProcessingResultType.Fail,%s);\n",
                entity.getName()));
        content.append("}\n");
        generateEntityPropertySettings(content,entity,false);
        content.append(String.format("%s.update(%s);\n",
                serviceName,
                entity.getName()
                ));
        content.append(String.format("return new ResultWithEntity<>(ProcessingResultType.Success,%s);\n",
                entity.getName()));
        content.append("}\n");

        /* delete method */
        content.append("@RequestMapping(value = \"/delete\",method = RequestMethod.POST,\n");
        content.append("produces = \"application/json\")\n");
        content.append("@ResponseBody\n");
        content.append(String.format("public Result delete(%s id) {\n",
                entity.getIdProperty().getType(),
                entity.getIdProperty().getName()
                ));
        content.append(String.format("%s.delete(id);\n",
                serviceName
                ));
        content.append("return new Result(ProcessingResultType.Success,\"删除成功\");\n");


        content.append("}\n");

        content.append("}\n");

        /*--*/

        return psiFileFactory.createFileFromText(controllerClassName + ".java", JavaLanguage.INSTANCE,
                content.toString());
    }

    private static void generateEntityPropertySettings(StringBuilder content, Entity entity, boolean withIdProperty) {
        for (Property property:entity.getProperties()){
            if (property == entity.getIdProperty() && !withIdProperty) {
                continue;
            }
            if (property.getPropertyType()== PropertyType.Column) {
                generateEntityPropertySetting(content,entity,(SingleProperty)property);
            }
        }

    }

    private static void generateEntityPropertySetting(StringBuilder content, Entity entity, SingleProperty property) {
        if (TypeUtils.isPrimitiveType(property.getType())){
            content.append(entity.getName());
            content.append(".");
            content.append(property.getSetter());
            content.append("(");
            switch(property.getType()) {
                case "boolean":
                    content.append(String.format("\"y\".equals(%s)",
                            property.getName()+"Val"));
                    break;
                default:
                content.append(property.getName());
            }
            content.append(");\n");
        } else{
            String shortTypeName=TypeUtils.getShortTypeName(property.getType());
            if (property.getEnumType()!=null) {
                content.append(String.format("if (%s==null){\n",
                        property.getName()+"Val"));
                content.append(entity.getName());
                content.append(".");
                content.append(property.getSetter());
                content.append("(null);\n");
                content.append("} else {\n");
                content.append(entity.getName());
                content.append(".");
                content.append(property.getSetter());
                content.append("(");
                content.append(String.format("%s.values()[%s]",
                        shortTypeName,
                        property.getName()+"Val"));
                content.append(");\n");
                content.append("}\n");
            } else {
                switch (shortTypeName) {
                    case "Date":
                        content.append(entity.getName());
                        content.append(".");
                        content.append(property.getSetter());
                        content.append("(DateTools.parseDate(");
                        content.append(property.getName()+"Val");
                        content.append("));\n");
                        break;
                    case "Boolean":
                        content.append(String.format("if (%s==null){\n",
                                property.getName()+"Val"));
                        content.append(entity.getName());
                        content.append(".");
                        content.append(property.getSetter());
                        content.append("(null);\n");
                        content.append("} else {\n");
                        content.append(entity.getName());
                        content.append(".");
                        content.append(property.getSetter());
                        content.append("(");
                        content.append(String.format("\"y\".equals(%s)",
                                property.getName()+"Val"));
                        content.append(");\n");
                        content.append("}\n");
                        break;
                    default:
                        content.append(entity.getName());
                        content.append(".");
                        content.append(property.getSetter());
                        content.append("(");
                        content.append(property.getName());
                        content.append(");\n");
                }
            }

        }

    }

    private static void generateEntityParamList(StringBuilder content, Entity entity, boolean withIdProperty) {
        int i;
        i=0;
        for (Property property:entity.getProperties()){
            if (property == entity.getIdProperty() && !withIdProperty) {
                continue;
            }
            if (property.getPropertyType()== PropertyType.Column) {
                if (i!=0) {
                    content.append(" , ");
                }
                i++;
                generateEntityParam(content,entity,(SingleProperty)property);
            }
        }
    }

    private static void generateEntityParam(StringBuilder content, Entity entity, SingleProperty property) {
        if (property.getEnumType()!=null) {
            content.append("Integer ");
            content.append(property.getName()+"Val");
        } else {
            String shortTypeName=TypeUtils.getShortTypeName(property.getType());
            switch (shortTypeName) {
                case "Date":
                case "boolean":
                case "Boolean":
                    content.append("String ");
                    content.append(property.getName() + "Val");
                    break;
                default:
                    content.append(shortTypeName);
                    content.append(" ");
                    content.append(property.getName());
            }
        }
    }
}