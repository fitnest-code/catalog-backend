import os
import shutil
import re

def refine_service(service_root, base_package):
    java_root = os.path.join(service_root, "src/main/java", base_package.replace(".", "/"))
    if not os.path.exists(java_root):
        print(f"Skipping {service_root}, path not found.")
        return

    moves = []
    
    # Identify Mappers to move to 'mapper'
    # We look in 'dto' first as common place, but also check root for mappers
    for root, dirs, files in os.walk(java_root):
        rel_dir = os.path.relpath(root, java_root)
        for file in files:
            if file.endswith("Mapper.java"):
                moves.append((os.path.join(rel_dir, file), "mapper"))

    # Identify Service implementations to move to 'service/impl'
    service_dir = os.path.join(java_root, "service")
    if os.path.exists(service_dir):
        for file in os.listdir(service_dir):
            if file.endswith(".java"):
                file_path = os.path.join(service_dir, file)
                if os.path.isfile(file_path):
                    # Check if it's an interface or a class
                    with open(file_path, "r") as f:
                        content = f.read()
                        if "interface " in content:
                            print(f"Keeping interface {file} in service package")
                            continue
                        else:
                            moves.append((os.path.join("service", file), "service/impl"))

    package_mapping = {}
    
    # Execute moves and record package map
    for old_rel, new_layer in moves:
        old_full = os.path.join(java_root, old_rel)
        if not os.path.exists(old_full): continue
        
        file_name = os.path.basename(old_full)
        new_dir = os.path.join(java_root, new_layer)
        os.makedirs(new_dir, exist_ok=True)
        
        # Determine old package
        with open(old_full, "r") as f:
            for line in f:
                if line.strip().startswith("package "):
                    old_pkg = line.strip().split(" ")[1].rstrip(";")
                    new_pkg = f"{base_package}.{new_layer.replace('/', '.')}"
                    package_mapping[old_pkg + "." + file_name.replace(".java", "")] = new_pkg + "." + file_name.replace(".java", "")
                    # Also map the package for full package replacements
                    break
        
        target_path = os.path.join(new_dir, file_name)
        print(f"Moving {old_full} -> {target_path}")
        shutil.move(old_full, target_path)
        
        # Update package declaration in the moved file immediately
        with open(target_path, "r") as f:
            content = f.read()
        new_content = re.sub(r"package az\.fitnest\..*?;", f"package {base_package}.{new_layer.replace('/', '.')};", content)
        with open(target_path, "w") as f:
            f.write(new_content)

    # Update all imports across the service
    # We need to collect all class-level mappings
    for root, dirs, files in os.walk(java_root):
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                with open(file_path, "r") as f:
                    content = f.read()
                
                new_content = content
                # Sort mappings by length descending to avoid partial matches
                for old_full_class, new_full_class in sorted(package_mapping.items(), key=lambda x: len(x[0]), reverse=True):
                    # Replace imports
                    new_content = new_content.replace(f"import {old_full_class};", f"import {new_full_class};")
                    # Replace fully qualified names
                    # We use word boundaries for FQNs
                    new_content = re.sub(rf"\b{re.escape(old_full_class)}\b", new_full_class, new_content)
                
                if new_content != content:
                    with open(file_path, "w") as f:
                        f.write(new_content)

# Run for all services
refine_service("/Users/aousganeh/Desktop/fix/code/catalog-service", "az.fitnest.catalog")
refine_service("/Users/aousganeh/Desktop/fix/code/identity-service", "az.fitnest.identity")
refine_service("/Users/aousganeh/Desktop/fix/code/user-service", "az.fitnest.user")
