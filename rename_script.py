import os
import shutil

project_dir = "/Users/geremiasmarquez/AndroidStudioProjects/PomodoroTimer"

# Directories to move
dirs_to_move = [
    "app/src/main/java/com/matias/pomodoro",
    "app/src/androidTest/java/com/matias/pomodoro",
    "app/src/test/java/com/matias/pomodoro"
]

for d in dirs_to_move:
    src = os.path.join(project_dir, d)
    dst = os.path.join(project_dir, d.replace("pomodoro", "pomodoro"))
    if os.path.exists(src):
        os.rename(src, dst)
        print(f"Renamed directory {src} to {dst}")

# Files to skip
skip_dirs = {'.git', '.gradle', 'build', '.idea'}

# Walk through files and replace text
for root, dirs, files in os.walk(project_dir):
    # Remove skip directories
    dirs[:] = [d for d in dirs if d not in skip_dirs]
    for file in files:
        if file.endswith((".kt", ".xml", ".kts", ".txt", ".md", ".json")):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            new_content = content
            # Replace package / namespace
            new_content = new_content.replace('com.matias.pomodoro', 'com.matias.pomodoro')
            
            # App name in strings.xml
            if file == "strings.xml":
                new_content = new_content.replace('>Pomodoro<', '>Pomodoro<')

            # MainScreen title
            if file == "MainScreen.kt":
                new_content = new_content.replace('text  = "pomodoro"', 'text  = "pomodoro"')

            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"Updated {filepath}")

