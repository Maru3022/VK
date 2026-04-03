box.cfg{ listen = 3301 }

box.schema.space.create('VK', {if_not_exists = true})

box.space.VK:format({
  {name = 'key',   type = 'string'},
  {name = 'value', type = 'varbinary', is_nullable = true}
})

box.space.VK:create_index('primary', {
  type = 'TREE',
  parts = {'key'},
  unique = true,
  if_not_exists = true
})

box.schema.user.grant('guest', 'read,write,execute', 'universe', 
  nil, {if_not_exists = true})
