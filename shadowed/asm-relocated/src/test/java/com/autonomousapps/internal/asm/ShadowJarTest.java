// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.asm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

public class ShadowJarTest {

  @Test
  public void test() {
    List<String> entries = new ArrayList<>();

    try (ZipInputStream zip = new ZipInputStream(requireNonNull(getClass().getResourceAsStream("/shadow.jar")))) {
      ZipEntry entry = zip.getNextEntry();
      while (entry != null) {
        String name = entry.getName();
        if (!entry.isDirectory()) {
          entries.add(name);
        }
        entry = zip.getNextEntry();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to open 'shadow.jar'", e);
    }

    assertThat(entries.stream().sorted().collect(Collectors.toUnmodifiableList())).containsExactly(
      "META-INF/MANIFEST.MF",
      "com/autonomousapps/internal/asm/AnnotationVisitor.class",
      "com/autonomousapps/internal/asm/AnnotationWriter.class",
      "com/autonomousapps/internal/asm/Attribute$Set.class",
      "com/autonomousapps/internal/asm/Attribute.class",
      "com/autonomousapps/internal/asm/ByteVector.class",
      "com/autonomousapps/internal/asm/ClassReader.class",
      "com/autonomousapps/internal/asm/ClassTooLargeException.class",
      "com/autonomousapps/internal/asm/ClassVisitor.class",
      "com/autonomousapps/internal/asm/ClassWriter.class",
      "com/autonomousapps/internal/asm/ConstantDynamic.class",
      "com/autonomousapps/internal/asm/Constants.class",
      "com/autonomousapps/internal/asm/Context.class",
      "com/autonomousapps/internal/asm/CurrentFrame.class",
      "com/autonomousapps/internal/asm/Edge.class",
      "com/autonomousapps/internal/asm/FieldVisitor.class",
      "com/autonomousapps/internal/asm/FieldWriter.class",
      "com/autonomousapps/internal/asm/Frame.class",
      "com/autonomousapps/internal/asm/Handle.class",
      "com/autonomousapps/internal/asm/Handler.class",
      "com/autonomousapps/internal/asm/Label.class",
      "com/autonomousapps/internal/asm/MethodTooLargeException.class",
      "com/autonomousapps/internal/asm/MethodVisitor.class",
      "com/autonomousapps/internal/asm/MethodWriter.class",
      "com/autonomousapps/internal/asm/ModuleVisitor.class",
      "com/autonomousapps/internal/asm/ModuleWriter.class",
      "com/autonomousapps/internal/asm/Opcodes.class",
      "com/autonomousapps/internal/asm/RecordComponentVisitor.class",
      "com/autonomousapps/internal/asm/RecordComponentWriter.class",
      "com/autonomousapps/internal/asm/Symbol.class",
      "com/autonomousapps/internal/asm/SymbolTable$Entry.class",
      "com/autonomousapps/internal/asm/SymbolTable$LabelEntry.class",
      "com/autonomousapps/internal/asm/SymbolTable.class",
      "com/autonomousapps/internal/asm/Type.class",
      "com/autonomousapps/internal/asm/TypePath.class",
      "com/autonomousapps/internal/asm/TypeReference.class",
      "com/autonomousapps/internal/asm/signature/SignatureReader.class",
      "com/autonomousapps/internal/asm/signature/SignatureVisitor.class",
      "com/autonomousapps/internal/asm/signature/SignatureWriter.class",
      "com/autonomousapps/internal/asm/tree/AbstractInsnNode.class",
      "com/autonomousapps/internal/asm/tree/AnnotationNode.class",
      "com/autonomousapps/internal/asm/tree/ClassNode.class",
      "com/autonomousapps/internal/asm/tree/FieldInsnNode.class",
      "com/autonomousapps/internal/asm/tree/FieldNode.class",
      "com/autonomousapps/internal/asm/tree/FrameNode.class",
      "com/autonomousapps/internal/asm/tree/IincInsnNode.class",
      "com/autonomousapps/internal/asm/tree/InnerClassNode.class",
      "com/autonomousapps/internal/asm/tree/InsnList$InsnListIterator.class",
      "com/autonomousapps/internal/asm/tree/InsnList.class",
      "com/autonomousapps/internal/asm/tree/InsnNode.class",
      "com/autonomousapps/internal/asm/tree/IntInsnNode.class",
      "com/autonomousapps/internal/asm/tree/InvokeDynamicInsnNode.class",
      "com/autonomousapps/internal/asm/tree/JumpInsnNode.class",
      "com/autonomousapps/internal/asm/tree/LabelNode.class",
      "com/autonomousapps/internal/asm/tree/LdcInsnNode.class",
      "com/autonomousapps/internal/asm/tree/LineNumberNode.class",
      "com/autonomousapps/internal/asm/tree/LocalVariableAnnotationNode.class",
      "com/autonomousapps/internal/asm/tree/LocalVariableNode.class",
      "com/autonomousapps/internal/asm/tree/LookupSwitchInsnNode.class",
      "com/autonomousapps/internal/asm/tree/MethodInsnNode.class",
      "com/autonomousapps/internal/asm/tree/MethodNode$1.class",
      "com/autonomousapps/internal/asm/tree/MethodNode.class",
      "com/autonomousapps/internal/asm/tree/ModuleExportNode.class",
      "com/autonomousapps/internal/asm/tree/ModuleNode.class",
      "com/autonomousapps/internal/asm/tree/ModuleOpenNode.class",
      "com/autonomousapps/internal/asm/tree/ModuleProvideNode.class",
      "com/autonomousapps/internal/asm/tree/ModuleRequireNode.class",
      "com/autonomousapps/internal/asm/tree/MultiANewArrayInsnNode.class",
      "com/autonomousapps/internal/asm/tree/ParameterNode.class",
      "com/autonomousapps/internal/asm/tree/RecordComponentNode.class",
      "com/autonomousapps/internal/asm/tree/TableSwitchInsnNode.class",
      "com/autonomousapps/internal/asm/tree/TryCatchBlockNode.class",
      "com/autonomousapps/internal/asm/tree/TypeAnnotationNode.class",
      "com/autonomousapps/internal/asm/tree/TypeInsnNode.class",
      "com/autonomousapps/internal/asm/tree/UnsupportedClassVersionException.class",
      "com/autonomousapps/internal/asm/tree/Util.class",
      "com/autonomousapps/internal/asm/tree/VarInsnNode.class"
    ).inOrder();
  }
}
