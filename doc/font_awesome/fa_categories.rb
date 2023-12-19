require 'yaml'
puts "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
puts "<resources>"
categories.each do | key, value |
  name = key.gsub("-", "_")
  puts "<string name=\"category_#{name}_label\">\"#{value['label'].gsub("&","&amp;")}\"</string>"
  puts "<string-array name=\"category_#{name}_icons\">"
  value['icons'].each do | icon |
    puts "<item>#{icon}</item>"
  end
  puts "</string-array>"
end
puts "</resources>"
