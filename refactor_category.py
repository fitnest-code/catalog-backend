import os
import glob

# Paths
base_dir = '/Users/aousganeh/Desktop/fitnest/code/catalog-backend/src/main/java/az/fitnest/catalog'
dto_dir = os.path.join(base_dir, 'dto')
request_dir = os.path.join(dto_dir, 'request')
response_dir = os.path.join(dto_dir, 'response')

os.makedirs(request_dir, exist_ok=True)
os.makedirs(response_dir, exist_ok=True)

# 1. Move and rename DTOs
req_old = os.path.join(dto_dir, 'CategoryRequest.java')
req_new = os.path.join(request_dir, 'CategoryRequest.java')
resp_old = os.path.join(dto_dir, 'CategoryDto.java')
resp_new = os.path.join(response_dir, 'CategoryResponse.java')

if os.path.exists(req_old): os.rename(req_old, req_new)
if os.path.exists(resp_old): os.rename(resp_old, resp_new)

# 2. Update the actual DTO files
def update_file(path, replacements):
    if not os.path.exists(path): return
    with open(path, 'r') as f: content = f.read()
    for old, new in replacements: content = content.replace(old, new)
    with open(path, 'w') as f: f.write(content)

update_file(req_new, [
    ('package az.fitnest.catalog.dto;', 'package az.fitnest.catalog.dto.request;')
])
update_file(resp_new, [
    ('package az.fitnest.catalog.dto;', 'package az.fitnest.catalog.dto.response;'),
    ('CategoryDto', 'CategoryResponse')
])

# 3. Traverse all java files and replace references
java_files = glob.glob(base_dir + '/**/*.java', recursive=True)
for file_path in java_files:
    # Skip the actual DTO files we just modified
    if file_path in [req_new, resp_new]: continue
    
    with open(file_path, 'r') as f:
        content = f.read()
        
    original = content
    content = content.replace('import az.fitnest.catalog.dto.CategoryDto;', 'import az.fitnest.catalog.dto.response.CategoryResponse;')
    content = content.replace('import az.fitnest.catalog.dto.CategoryRequest;', 'import az.fitnest.catalog.dto.request.CategoryRequest;')
    content = content.replace('CategoryDto', 'CategoryResponse')
    
    if original != content:
        with open(file_path, 'w') as f:
            f.write(content)
