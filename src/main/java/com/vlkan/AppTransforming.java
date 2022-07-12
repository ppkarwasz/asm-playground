package com.vlkan;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.StaticInitMerger;

public class AppTransforming {

    private static final Type STACK_TRACE_ARRAY = Type.getType(StackTraceElement[].class);
    private static final Type STACK_TRACE_ELEMENT = Type.getType(StackTraceElement.class);
    private static final Type LOG_BUILDER_UTIL = Type.getType(LogBuilderUtil.class);
    private static final String LOCATIONS_FIELD = "$log4j2$locations";

    public static void main(String[] args) throws Exception {
        injectSourceLocation("com.vlkan.AppActual");
        AppActual.main(args);
    }

    private static void injectSourceLocation(String className) throws Exception {
        ClassReader cr = new ClassReader(className);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new StaticInitMerger(Opcodes.ASM9, "$log4j2$init", cw) {

            private String fileName;

            private String className;

            private List<StackTraceElement> locations = new ArrayList<>();

            @Override
            public void visit(int ver, int acc, String name, String sig, String superName, String[] ifs) {
                super.visit(ver, acc, name, sig, superName, ifs);
                className = name.replace('/', '.');
            }

            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                if (fileName == null) {
                    fileName = source;
                }
            }

            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor parentMethodVisitor = super.visitMethod(acc, name, desc, sig, ex);
                return new SourceLocationInjectingMethodVisitor(parentMethodVisitor, acc, desc,
                        fileName, className, name, locations);
            }

            @Override
            public void visitEnd() {
                visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, LOCATIONS_FIELD, STACK_TRACE_ARRAY.getDescriptor(), null, null);
                final GeneratorAdapter generator = new GeneratorAdapter(super.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, "<clinit>", "()V", null, null),
                        Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, "<clinit>", "()V");
                generator.visitMaxs(10, 0);
                generator.push(locations.size());
                generator.newArray(STACK_TRACE_ELEMENT);
                for (int i = 0; i < locations.size(); i++) {
                    final StackTraceElement location = locations.get(i);
                    generator.dup();
                    generator.push(i);
                    generator.newInstance(STACK_TRACE_ELEMENT);
                    generator.dup();
                    generator.push(location.getClassName());
                    generator.push(location.getMethodName());
                    generator.push(location.getFileName());
                    generator.push(location.getLineNumber());
                    generator.invokeConstructor(STACK_TRACE_ELEMENT, Method.getMethod("void <init>(String, String, String, int)"));
                    generator.arrayStore(STACK_TRACE_ELEMENT);
                }
                generator.putStatic(Type.getType("L" + className.replace(".", "/") + ";"), LOCATIONS_FIELD, STACK_TRACE_ARRAY);
                generator.returnValue();
                generator.endMethod();
                super.visitEnd();
            }

        }, 0);

        try (final OutputStream os = Files.newOutputStream(Paths.get("target", "AppTransformed.class"))) {
            os.write(cw.toByteArray());
        }
        MethodHandles.lookup().defineClass(cw.toByteArray());

    }

    private static final class SourceLocationInjectingMethodVisitor extends GeneratorAdapter {


        private final String fileName;

        private final String className;

        private final Type classType;

        private final String methodName;

        private int lineNumber;

        private List<StackTraceElement> locations;

        private SourceLocationInjectingMethodVisitor(
                MethodVisitor parentMethodVisitor,
                int access,
                String desc,
                String fileName,
                String className,
                String methodName,
                List<StackTraceElement> locations) {
            super(Opcodes.ASM9, parentMethodVisitor, access, methodName, desc);
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.locations = locations;
            this.classType = Type.getType("L" + className.replace(".", "/") + ";");
        }

        @Override
        public void visitLineNumber(int lineNumber, Label start) {
            super.visitLineNumber(lineNumber, start);
            this.lineNumber = lineNumber;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            boolean loggerCalled = Opcodes.INVOKEINTERFACE == opcode &&
                    "org/apache/logging/log4j/Logger".equals(owner) &&
                    "debug".equals(name) &&
                    "(Ljava/lang/String;)V".equals(descriptor);
            if (loggerCalled) {
                injectSourceLocation();
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }

        private void injectSourceLocation() {
            locations.add(new StackTraceElement(className, methodName, fileName, lineNumber));
            getStatic(classType, LOCATIONS_FIELD, STACK_TRACE_ARRAY);
            push(locations.size() - 1);
            arrayLoad(STACK_TRACE_ELEMENT);
            invokeStatic(LOG_BUILDER_UTIL,
                    Method.getMethod("void debug(org.apache.logging.log4j.Logger, String, StackTraceElement)"));
        }

    }

}
