require 'yaml'
puts "package org.totschnig.myexpenses.viewmodel.data"
puts
puts "import org.totschnig.myexpenses.R"
puts
puts "val FontAwesomeIcons: Map<String, IconInfo> = mapOf("
icons = YAML.load_file('icons.yml')
icons.each do | key, value |
  name = key.gsub("-", "_")
  puts "\"#{key}\" to IconInfo('\\u#{value['unicode'].rjust(4, "0")}', R.string.fa_#{name}_label, #{value['styles'].include?("brands")}),"
end
puts ")"
