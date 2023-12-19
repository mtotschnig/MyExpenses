require 'yaml'
puts "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
puts "<resources>"
icons = YAML.load_file('icons.yml')
icons.each do | key, value |
  name = key.gsub("-", "_")
  #puts "<string name=\"fa_#{name}_unicode\">\"&#x#{value['unicode']};\"</string>"
  puts "<string name=\"fa_#{name}_label\">\"#{value['label'].gsub("&","&amp;")}\"</string>"
  #puts "<integer name=\"fa_#{name}_style\">#{value['styles'].include?("brands") ? "1" : "0"}</integer>" 
end
puts "</resources>"
