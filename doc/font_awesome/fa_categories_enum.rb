require 'yaml'
categories = YAML.load_file('categories.yml')
categories.each do | key, value |
  name = key.gsub("-", "_")
  object = key.split("-").map {|string| string.capitalize }.join()
  puts "data object #{object}: IconCategory(R.string.category_#{name}_label, R.array.category_#{name}_icons)"
end
