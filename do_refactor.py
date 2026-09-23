import os
import shutil

src_base = "app/src/main/java/com/example"
dest_base = "app/src/main/java/com/aistudio/executivogo/trnsp"

os.makedirs(dest_base, exist_ok=True)

# Walk through src_base and copy/move files
for root, dirs, files in os.walk(src_base):
    # Skip auto directory
    if "auto" in root:
        continue
    rel_path = os.path.relpath(root, src_base)
    target_dir = os.path.join(dest_base, rel_path if rel_path != "." else "")
    os.makedirs(target_dir, exist_ok=True)
    
    for file in files:
        if file == "ExecutivoCarAppService.kt" or file == "ExecutivoCarScreen.kt" or file == "ExecutivoGoMessagingService.kt":
            continue # Skip Android Auto and duplicate messaging service
        src_file = os.path.join(root, file)
        dest_file = os.path.join(target_dir, file)
        shutil.copy2(src_file, dest_file)

# Also handle test files if needed
for test_dir in ["app/src/test/java/com/example", "app/src/androidTest/java/com/example"]:
    if os.path.exists(test_dir):
        dest_test_dir = test_dir.replace("com/example", "com/aistudio/executivogo/trnsp")
        os.makedirs(dest_test_dir, exist_ok=True)
        for root, dirs, files in os.walk(test_dir):
            for file in files:
                src_file = os.path.join(root, file)
                dest_file = os.path.join(dest_test_dir, file)
                shutil.copy2(src_file, dest_file)

# Now replace com.example with com.aistudio.executivogo.trnsp in all java/kotlin files across app/src
for root, dirs, files in os.walk("app/src"):
    for file in files:
        if file.endswith(".kt") or file.endswith(".java") or file.endswith(".xml"):
            filepath = os.path.join(root, file)
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    content = f.read()
                
                new_content = content.replace("com.example.ui.theme", "com.aistudio.executivogo.trnsp.ui.theme")
                new_content = new_content.replace("com.example", "com.aistudio.executivogo.trnsp")
                
                if new_content != content:
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(new_content)
                    print(f"Updated: {filepath}")
            except Exception as e:
                print(f"Error reading {filepath}: {e}")

print("Refactoring script completed successfully.")
